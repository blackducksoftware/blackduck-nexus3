package com.synopsys.integration.blackduck.nexus3.mock.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobMetrics;
import org.sonatype.nexus.blobstore.api.BlobStore;

import com.synopsys.integration.test.TestResourceLoader;

public class MockBlob implements Blob {
    public static final String NEXUS_JAR = "nexus3Test.jar";
    private final Map<String, String> blobHeaders;

    public MockBlob() {
        blobHeaders = new HashMap<>();
        blobHeaders.put(BlobStore.BLOB_NAME_HEADER, NEXUS_JAR);
    }

    public MockBlob(final Map<String, String> blobHeaders) {
        this.blobHeaders = blobHeaders;
    }

    @Override
    public BlobId getId() {
        return null;
    }

    @Override
    public Map<String, String> getHeaders() {
        return blobHeaders;
    }

    @Override
    public InputStream getInputStream() {
        try {
            final File propertiesFile = new File(TestResourceLoader.DEFAULT_RESOURCE_DIR, NEXUS_JAR);
            return new FileInputStream(propertiesFile);
        } catch (final FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public BlobMetrics getMetrics() {
        return null;
    }
}
