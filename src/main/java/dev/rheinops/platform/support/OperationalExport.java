package dev.rheinops.platform.support;

public record OperationalExport(String filename, byte[] content) {

    public OperationalExport {
        content = content.clone();
    }

    @Override
    public byte[] content() {
        return content.clone();
    }
}
