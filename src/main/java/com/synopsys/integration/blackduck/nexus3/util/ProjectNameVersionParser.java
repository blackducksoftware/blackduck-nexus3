package com.synopsys.integration.blackduck.nexus3.util;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;

import com.synopsys.integration.util.NameVersion;

@Named
@Singleton
public class ProjectNameVersionParser {

    public String getProjectName(final Asset asset) {
        final NameVersion nameVersion = retrieveNameVersion(asset);
        return nameVersion.getName();
    }

    public String getProjectVersion(final Asset asset) {
        final NameVersion nameVersion = retrieveNameVersion(asset);
        return nameVersion.getVersion();
    }

    public NameVersion retrieveNameVersion(final Asset asset) {
        final String name = asset.name();
        final String[] nameParts = name.split("/");
        String projectName = "Unknown";
        String projectVersion = "Unknown";
        if (nameParts.length >= 3) {
            projectName = nameParts[nameParts.length - 3];
            projectVersion = nameParts[nameParts.length - 2];
        }
        return new NameVersion(projectName, projectVersion);
    }

    public NameVersion retrieveNameVersion(final Component component) {
        final String name = component.name();
        final String version = component.version();
        return new NameVersion(name, version);
    }

    public String retrieveBlobName(final Blob blob) {
        final String name = blob.getHeaders().get(BlobStore.BLOB_NAME_HEADER);
        final String[] nameParts = name.split("/");
        return nameParts[nameParts.length - 1];
    }
}
