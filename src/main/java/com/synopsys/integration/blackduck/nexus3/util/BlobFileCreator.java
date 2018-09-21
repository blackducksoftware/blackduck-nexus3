package com.synopsys.integration.blackduck.nexus3.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;

import com.synopsys.integration.blackduck.nexus3.database.QueryManager;

@Named
@Singleton
public class BlobFileCreator {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ProjectNameVersionParser projectNameVersionParser;
    private final QueryManager queryManager;

    @Inject
    public BlobFileCreator(final ProjectNameVersionParser projectNameVersionParser, final QueryManager queryManager) {
        this.projectNameVersionParser = projectNameVersionParser;
        this.queryManager = queryManager;
    }

    public File convertBlobToFile(final Blob blob, final File parentDirectory) throws IOException {
        final String blobName = projectNameVersionParser.retrieveBlobName(blob);

        final InputStream blobInputStream = blob.getInputStream();
        final File blobFile = new File(parentDirectory, blobName);
        FileUtils.copyInputStreamToFile(blobInputStream, blobFile);

        return blobFile;
    }

    public Optional<File> convertAssetToFile(final Asset asset, final Repository repository, final File parentDirectory) {
        if (asset.blobRef() != null) {
            final Blob binaryBlob = queryManager.getBlob(repository, asset.blobRef());
            logger.debug("Binary blob header contents: {}", binaryBlob.getHeaders().toString());
            try {
                final File binaryFile = convertBlobToFile(binaryBlob, parentDirectory);
                return Optional.of(binaryFile);
            } catch (final IOException e) {
                logger.error("Error saving blob to file {}", e.getMessage());
            }
        }

        return Optional.empty();
    }
}
