package io.jenkins.plugins.plugin;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class SimplePlugin extends BuildWrapper {

    private static final String REPORT_TEMPLATE_PATH = "/index.html";
    private static final String PROJECT_NAME_VAR = "$PROJECT_NAME$";
    private static final String BUILD_NUMBER_VAR = "$BUILD_NUMBER$";

    @DataBoundConstructor
    public SimplePlugin() {
    }

    @Override
    public Environment setUp(AbstractBuild build, final Launcher launcher, BuildListener listener) {
        return new Environment() {
            @Override
            public boolean tearDown(AbstractBuild build, BuildListener listener)
                    throws IOException, InterruptedException {
                String report = generateFile(build.getProject().getDisplayName(), build.number);
                File artifactsDir = build.getArtifactsDir();
                if (!artifactsDir.isDirectory()) {
                    boolean success = artifactsDir.mkdirs();
                    if (!success) {
                        listener.getLogger().println("Can't create artifacts directory at "
                                + artifactsDir.getAbsolutePath());
                    }
                }
                String path = artifactsDir.getCanonicalPath() + REPORT_TEMPLATE_PATH;
                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path),
                        StandardCharsets.UTF_8))) {
                    writer.write(report);
                }
                return super.tearDown(build, listener);
            }
        };
    }


    private static String generateFile(String projectName, int buildNumber) throws IOException {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        try (InputStream in = SimplePlugin.class.getResourceAsStream(REPORT_TEMPLATE_PATH)) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                bOut.write(buffer, 0, read);
            }
        }
        String content = new String(bOut.toByteArray(), StandardCharsets.UTF_8);
        content = content.replace(PROJECT_NAME_VAR, projectName);
        content = content.replace(BUILD_NUMBER_VAR, String.valueOf(buildNumber));
        return content;
    }

    @Extension
    public static final class DescriptorImpl extends BuildWrapperDescriptor {

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Print Project Name And Build Number";
        }

    }


}