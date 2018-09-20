package com.synopsys.integration.blackduck.nexus3.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.api.Blob;

@Named
@Singleton
public class BlobFileCreator {
    private final ProjectNameVersionParser projectNameVersionParser;

    public BlobFileCreator(final ProjectNameVersionParser projectNameVersionParser) {
        this.projectNameVersionParser = projectNameVersionParser;
    }

    public File convertBlobToFile(final Blob blob, final File parentDirectory) throws IOException {
        final String blobName = projectNameVersionParser.retrieveBlobName(blob);

        final InputStream blobInputStream = blob.getInputStream();
        final byte[] buffer = new byte[blobInputStream.available()];
        blobInputStream.read(buffer);

        final File blobFile = new File(parentDirectory, blobName);
        final FileOutputStream blobOutputStream = new FileOutputStream(blobFile);
        blobOutputStream.write(buffer);

        return blobFile;
    }
}
