package com.synopsys.integration.blackduck.nexus3.util;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobStore;

import com.synopsys.integration.util.NameVersion;

@Named
@Singleton
public class ProjectNameVersionParser {

    public String getProjectName(final String assetName) {
        final NameVersion nameVersion = retrieveNameVersion(assetName);
        return nameVersion.getName();
    }

    public String getProjectVersion(final String assetName) {
        final NameVersion nameVersion = retrieveNameVersion(assetName);
        return nameVersion.getVersion();
    }

    public NameVersion retrieveNameVersion(final String assetName) {
        final String[] nameParts = assetName.split("/");
        String projectName = "Unknown";
        String projectVersion = "Unknown";
        if (nameParts.length >= 3) {
            projectName = nameParts[nameParts.length - 3];
            projectVersion = nameParts[nameParts.length - 2];
        }
        return new NameVersion(projectName, projectVersion);
    }

    public String retrieveBlobName(final Blob blob) {
        final String name = blob.getHeaders().get(BlobStore.BLOB_NAME_HEADER);
        final String[] nameParts = name.split("/");
        return nameParts[nameParts.length - 1];
    }
}
