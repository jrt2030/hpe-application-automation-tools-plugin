// (C) Copyright 2003-2015 Hewlett-Packard Development Company, L.P.

package com.hp.octane.plugins.jenkins;

import com.google.inject.Inject;
import com.hp.octane.plugins.jenkins.configuration.ConfigurationService;
import com.hp.octane.plugins.jenkins.tests.LockoutModel;
import hudson.Extension;
import hudson.Plugin;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.Scrambler;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

public class OctanePlugin extends Plugin implements Describable<OctanePlugin> {

    private String identity;

    private String location;
    private String domain;
    private String project;
    private String username;
    private String password;

    public String getIdentity() {
        return identity;
    }

    @Override
    public void postInitialize() throws IOException {
        if (identity == null) {
            this.identity = UUID.randomUUID().toString();
            save();
        }
    }

    @Override
    public void configure(StaplerRequest req, JSONObject formData) throws IOException {
        location = (String) formData.get("location");
        domain = (String) formData.get("domain");
        project = (String) formData.get("project");
        username = (String) formData.get("username");
        password = Scrambler.scramble((String) formData.get("password"));
        save();
    }

    @Override
    public Descriptor<OctanePlugin> getDescriptor() {
        return new OctanePluginDescriptor();
    }

    public String getLocation() {
        return location;
    }

    public String getDomain() {
        return domain;
    }

    public String getProject() {
        return project;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return Scrambler.descramble(password);
    }

    @Extension
    public static final class OctanePluginDescriptor extends Descriptor<OctanePlugin> {

        private OctanePlugin octanePlugin;

        @Inject
        private LockoutModel lockoutModel;

        public OctanePluginDescriptor() {
            octanePlugin = Jenkins.getInstance().getPlugin(OctanePlugin.class);
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            try {
                octanePlugin.configure(req, formData);
                return true;
            } catch (IOException e) {
                throw new FormException(e, Messages.ConfigurationSaveFailed());
            }
        }

        public FormValidation doCheckLocation(@QueryParameter String value) {
            if (value.isEmpty()) {
                return FormValidation.error(Messages.ConfigurationUrlNotSpecified());
            }
            try {
                new URL(value);
                return FormValidation.ok();
            } catch (MalformedURLException e) {
                return FormValidation.error(Messages.ConfigurationUrInvalid());
            }
        }

        public FormValidation doCheckDomain(@QueryParameter String value) {
            if (value.isEmpty()) {
                return FormValidation.error(Messages.ConfigurationDomainNotSpecified());
            } else {
                return FormValidation.ok();
            }
        }

        public FormValidation doCheckProject(@QueryParameter String value) {
            if (value.isEmpty()) {
                return FormValidation.error(Messages.ConfigurationProjectNotSpecified());
            } else {
                return FormValidation.ok();
            }
        }

        public FormValidation doTestGlobalConnection(@QueryParameter("location") String location,
                                                     @QueryParameter("domain") String domain,
                                                     @QueryParameter("project") String project,
                                                     @QueryParameter("username") String username,
                                                     @QueryParameter("password") String password) {
            FormValidation validation = ConfigurationService.checkConfiguration(location, domain, project, username, password);
            if (validation.kind == FormValidation.Kind.OK &&
                    location.equals(octanePlugin.getLocation()) &&
                    username.equals(octanePlugin.getUsername()) &&
                    password.equals(octanePlugin.getPassword())) {
                    lockoutModel.success();
            }
            return validation;
        }

        @Override
        public String getDisplayName() {
            return Messages.PluginName();
        }

        public String getLocation() {
            return octanePlugin.getLocation();
        }

        public String getDomain() {
            return octanePlugin.getDomain();
        }

        public String getProject() {
            return octanePlugin.getProject();
        }

        public String getUsername() {
            return octanePlugin.getUsername();
        }

        public String getPassword() {
            return octanePlugin.getPassword();
        }
    }
}
