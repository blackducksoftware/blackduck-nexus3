package com.synopsys.integration.blackduck.nexus3.mock.model;

import javax.annotation.Nullable;

import org.joda.time.DateTime;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.EntityMetadata;
import org.sonatype.nexus.repository.storage.Component;

public class MockComponent implements Component {
    private String group;
    private String name;
    private String version;

    public MockComponent() {
        this("testGroup", "testName", "testVersion");
    }

    public MockComponent(final String group, final String name, final String version) {
        this.group = group;
        this.name = name;
        this.version = version;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Component name(final String name) {
        this.name = name;
        return this;
    }

    @Nullable
    @Override
    public String group() {
        return group;
    }

    @Override
    public String requireGroup() {
        return null;
    }

    @Override
    public Component group(@Nullable final String group) {
        this.group = group;
        return this;
    }

    @Nullable
    @Override
    public String version() {
        return version;
    }

    @Override
    public String requireVersion() {
        return null;
    }

    @Override
    public Component version(@Nullable final String version) {
        this.version = version;
        return this;
    }

    @Override
    public boolean isNew() {
        return false;
    }

    @Override
    public Component newEntity(final boolean newEntity) {
        return null;
    }

    @Override
    public EntityId bucketId() {
        return null;
    }

    @Override
    public Component bucketId(final EntityId bucketId) {
        return null;
    }

    @Nullable
    @Override
    public DateTime lastUpdated() {
        return null;
    }

    @Override
    public DateTime requireLastUpdated() {
        return null;
    }

    @Override
    public Component lastUpdated(final DateTime lastUpdated) {
        return null;
    }

    @Override
    public String format() {
        return null;
    }

    @Override
    public Component format(final String format) {
        return null;
    }

    @Override
    public NestedAttributesMap attributes() {
        return null;
    }

    @Override
    public Component attributes(final NestedAttributesMap attributes) {
        return null;
    }

    @Override
    public NestedAttributesMap formatAttributes() {
        return null;
    }

    @Nullable
    @Override
    public EntityMetadata getEntityMetadata() {
        return null;
    }

    @Override
    public void setEntityMetadata(@Nullable final EntityMetadata entityMetadata) {

    }
}
