package com.synopsys.integration.blackduck.nexus3.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.FileUtils;
import org.sonatype.nexus.blobstore.api.Blob;

@Named
@Singleton
public class BlobConverter {
    private final ProjectNameVersionParser projectNameVersionParser;

    @Inject
    public BlobConverter(final ProjectNameVersionParser projectNameVersionParser) {
        this.projectNameVersionParser = projectNameVersionParser;
    }

    public File convertBlobToFile(final Blob blob, final File parentDirectory) throws IOException {
        final String blobName = projectNameVersionParser.retrieveBlobName(blob);

        final InputStream blobInputStream = blob.getInputStream();
        final File blobFile = new File(parentDirectory, blobName);
        FileUtils.copyInputStreamToFile(blobInputStream, blobFile);

        return blobFile;
    }

}
