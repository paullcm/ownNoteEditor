/*
 * Copyright (c) 2014ff Thomas Feuster
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE VERSIONS ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE VERSIONS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package tf.ownnote.ui.notes;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import org.apache.commons.codec.binary.Base64;
import tf.ownnote.ui.tags.TagInfo;
import tf.ownnote.ui.tags.TagManager;

/**
 * Store for note metadata, e.g. author, last modified, tags, ...
 * 
 * @author thomas
 */
public class NoteMetaData {
    private static final String META_STRING_PREFIX = "<!-- ";
    private static final String META_STRING_SUFFIX = " -->";
    private static final String META_DATA_SEP = "---";
    private static final String META_VALUES_SEP = ":::";
    
    private enum UpdateTag {
        LINK,
        UNLINK
    }
    
    private static final Deflater compresser = new Deflater(Deflater.BEST_COMPRESSION);
    private static final Inflater decompresser = new Inflater();

    private static enum Multiplicity {
        SINGLE,
        MULTIPLE
    }
    
    // info per available metadata - name & multiplicity
    private static enum MetaDataInfo {
        VERSIONS("versions", Multiplicity.MULTIPLE),
        TAGS("tags", Multiplicity.MULTIPLE),
        CHARSET("charset", Multiplicity.SINGLE);
        
        private final String dataName;
        private final Multiplicity dataMulti;
        
        private MetaDataInfo (final String name, final Multiplicity multi) {
            dataName = name;
            dataMulti = multi;
        }
        
        public String getDataName() {
            return dataName;
        }
        
        public Multiplicity getDataMultiplicity() {
            return dataMulti;
        }
    }

    private final ObservableList<NoteVersion> myVersions = FXCollections.<NoteVersion>observableArrayList();
    private final ObservableSet<TagInfo> myTags = FXCollections.<TagInfo>observableSet();
    // TFE, 20201217: add charset to metadata - since we switched to UTF-8 on 17.12.2020 we need to be able to handle old notes
    private Charset myCharset = StandardCharsets.ISO_8859_1;

    private Note myNote;
    
    public NoteMetaData() {
        super();

        // go, tell it to the mountains
        myTags.addListener((SetChangeListener.Change<? extends TagInfo> change) -> {
            // can happen e.g. when using constructor fromHtmlComment()
            if (myNote == null) {
                return;
            }
            
            if (change.wasAdded()) {
//                System.out.println("Linking note " + myNote.getNoteName() + " to tag " + change.getElementAdded().getName());
                change.getElementAdded().getLinkedNotes().add(myNote);
            }

            if (change.wasRemoved()) {
//                System.out.println("Unlinking note " + myNote.getNoteName() + " from tag " + change.getElementRemoved().getName());
                change.getElementRemoved().getLinkedNotes().remove(myNote);
            }
        });
    }
    
    public boolean isEmpty() {
        return (myVersions.isEmpty() && myTags.isEmpty());
    }

    public ObservableList<NoteVersion> getVersions() {
        return myVersions;
    }

    public void setVersions(final List<NoteVersion> versions) {
        myVersions.clear();
        myVersions.addAll(versions);
    }

    public NoteVersion getVersion() {
        if (!myVersions.isEmpty()) {
            return myVersions.get(myVersions.size()-1);
        } else {
            return null;
        }
    }

    public void addVersion(final NoteVersion version) {
        if (myVersions.isEmpty()) {
            myVersions.add(version);
        } else {
            // keep only one entry per date - otherwise its going to explode
            if (myVersions.get(myVersions.size() - 1).getDate().toLocalDate().equals(version.getDate().toLocalDate())) {
                myVersions.remove(myVersions.size() - 1);
            }
            myVersions.add(version);
        }
    }

    public ObservableSet<TagInfo> getTags() {
        return myTags;
    }

    public void setTags(final Set<TagInfo> tags) {
        myTags.clear();
        myTags.addAll(tags);
    }
    
    private void updateTags(final UpdateTag updateTag) {
        if (myNote == null) return;
        
        for (TagInfo tag : myTags) {
            switch (updateTag) {
                case LINK:
//                    System.out.println("Linking note " + myNote.getNoteName() + " to tag " + tag.getName());
                    tag.getLinkedNotes().add(myNote);
                    break;
                case UNLINK:
//                    System.out.println("Unlinking note " + myNote.getNoteName() + " to tag " + tag.getName());
                    // TFE; 20201227: for some obscure reason the following doesn't work - don't ask
                    tag.getLinkedNotes().remove(myNote);
                    break;
            }
        }
    }
    
    public Note getNote() {
        return myNote;
    }
    
    public void setNote(final Note note) {
        updateTags(UpdateTag.UNLINK);
        myNote = note;
        updateTags(UpdateTag.LINK);
    }
    
    public Charset getCharset() {
        return myCharset;
    }

    public void setCharset(final Charset charset) {
        myCharset = charset;
    }
    
    public static boolean hasMetaDataContent(final String htmlString) {
        if (htmlString == null) {
            return false;
        }
        
        final String contentString = htmlString.split("\n")[0];
        return (contentString.startsWith(META_STRING_PREFIX) && contentString.endsWith(META_STRING_SUFFIX));
    }
    
    public static String removeMetaDataContent(final String htmlString) {
        if (htmlString == null) {
            return "";
        }
        
        String result = htmlString;
        if (hasMetaDataContent(htmlString)) {
            final int endPos = htmlString.indexOf("\n");
            if (endPos < htmlString.length()) {
                result = htmlString.substring(endPos+1);
            } else {
                result = "";
            }
        }
        
        return result;
    }
    
    public static NoteMetaData fromHtmlComment(final String htmlString) {
        final NoteMetaData result = new NoteMetaData();

        // parse html string
        // everything inside a <!-- --> could be metadata in the form 
        // authors="xyz" tags="a:::b:::c"
        
        if (htmlString != null && hasMetaDataContent(htmlString)) {
            final String contentString = htmlString.split(META_STRING_SUFFIX)[0] + META_STRING_SUFFIX;
            String [] data = contentString.substring(META_STRING_PREFIX.length(), contentString.length()-META_STRING_SUFFIX.length()).
                    strip().split(META_DATA_SEP);
            
            // check for "data" first to decompress if required
            boolean dataFound = false;
            String dataString = "";
            for (String nameValue : data) {
                if (nameValue.startsWith("data=\"") && nameValue.endsWith("\"")) {
                    final String[] values = nameValue.substring("data".length()+2, nameValue.length()-1).
                        strip().split(META_VALUES_SEP);

                    decompresser.reset();
                    final byte[] decoded = Base64.decodeBase64(values[0]);
                    decompresser.setInput(decoded, 0, decoded.length);

                    final byte[] temp = new byte[32768];
                    try {
                        final int resultLength = decompresser.inflate(temp);

                        final byte[] input = new byte[resultLength];
                        System.arraycopy(temp, 0, input, 0, resultLength);
                        
                        dataString = new String(input, "UTF-8");
                        
                        dataFound = true;
                    } catch (DataFormatException | UnsupportedEncodingException ex) {
                        Logger.getLogger(NoteMetaData.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            if (dataFound) {
                data = dataString.strip().split(META_DATA_SEP);
            }

            // now we have the name - value pairs
            // split further depending on multiplicity
            for (String nameValue : data) {
                boolean infoFound = false;
                for (MetaDataInfo info : MetaDataInfo.values()) {
                    final String dataName = info.getDataName();
                    if (nameValue.startsWith(dataName + "=\"") && nameValue.endsWith("\"")) {
                        // found it! now check & parse for values
                        final String[] values = nameValue.substring(dataName.length()+2, nameValue.length()-1).
                            strip().split(META_VALUES_SEP);
                        
                        switch (info) {
                            case VERSIONS:
                                final List<NoteVersion> versions = new LinkedList<>();
                                for (String value : values) {
                                    versions.add(NoteVersion.fromHtmlString(value));
                                }
                                result.setVersions(versions);
                                infoFound = true;
                                break;
                            case TAGS:
                                result.setTags(TagManager.getInstance().tagsForNames(new HashSet<>(Arrays.asList(values)), null, true));
                                infoFound = true;
                                break;
                            case CHARSET:
                                result.setCharset(Charset.forName(values[0]));
                                break;
                            default:
                        }
                    }
                    if (infoFound) {
                        // done, lets check next data value
                        break;
                    }
                }
            }
        }

        return result;
    }
    
    public static String toHtmlComment(final NoteMetaData data) {
        if (data == null) {
            return "";
        }

        final StringBuffer result = new StringBuffer();
        
        result.append(MetaDataInfo.CHARSET.getDataName());
        result.append("=\"");
        result.append(data.getCharset().name());
        result.append("\"");
        if (data.getVersion() != null) {
            result.append(META_DATA_SEP);
            result.append(MetaDataInfo.VERSIONS.getDataName());
            result.append("=\"");
            result.append(data.getVersions().stream().map((t) -> {
                return NoteVersion.toHtmlString(t);
            }).collect(Collectors.joining(META_VALUES_SEP)));
            result.append("\"");
        }
        if (!data.getTags().isEmpty()) {
            result.append(META_DATA_SEP);
            result.append(MetaDataInfo.TAGS.getDataName());
            result.append("=\"");
            result.append(data.getTags().stream().map((t) -> {
                return t.getName();
            }).collect(Collectors.joining(META_VALUES_SEP)));
            result.append("\"");
        }
        
        try {
            compresser.reset();
            compresser.setInput(result.toString().getBytes("UTF-8"));
            compresser.finish();
            
            final byte[] temp = new byte[32768];
            final int compressedDataLength = compresser.deflate(temp);
            final byte[] output = new byte[compressedDataLength];
            System.arraycopy(temp, 0, output, 0, compressedDataLength);
            final String encodedResult = Base64.encodeBase64String(output);

            // lets compress - if it is really shorter :-)
            if (encodedResult.length() < result.length()) {
                result.delete(0, result.length());
                result.append("data");
                result.append("=\"");
                result.append(encodedResult);
                result.append("\"");
            }
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(NoteMetaData.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return META_STRING_PREFIX + result.toString() + META_STRING_SUFFIX + "\n";
    }
}
