/*******************************************************************************
 * Copyright 2012
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package de.tudarmstadt.ukp.clarin.webanno.webapp.page.project;

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.NoResultException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.link.DownloadLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.ApplicationUtils;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.CasToBratJson;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedTagSetContent;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedTagSets;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationType;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.webapp.support.EntityModel;
/**
 * A Panel Used to add Tagsets to a selected {@link Project}
 * @author Seid Muhie Yimam
 *
 */

public class ProjectTagSetsPanel
    extends Panel
{
    private static final long serialVersionUID = 7004037105647505760L;

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;
    @SpringBean(name = "documentRepository")
    private RepositoryService projectRepository;

    private class TagSetSelectionForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -1L;

        @SuppressWarnings({ "unchecked" })
        public TagSetSelectionForm(String id)
        {
            // super(id);
            super(id, new CompoundPropertyModel<SelectionModel>(new SelectionModel()));

            add(new Button("create", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    if (selectedProjectModel.getObject().getId() == 0) {
                        error("Project not yet created. Please save project details first!");
                    }
                    else {
                        TagSetSelectionForm.this.getModelObject().tagSet = null;
                        tagSetDetailForm.setModelObject(new TagSet());
                        tagSetDetailForm.setVisible(true);
                        tagSelectionForm.setVisible(false);
                        tagDetailForm.setVisible(false);
                    }
                }
            });

            add(new ListChoice<TagSet>("tagSet")
            {
                private static final long serialVersionUID = 1L;

                {
                    setChoices(new LoadableDetachableModel<List<TagSet>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<TagSet> load()
                        {
                            Project project = selectedProjectModel.getObject();
                            if (project.getId() != 0) {
                                return annotationService.listTagSets(project);
                            }
                            else {
                                return new ArrayList<TagSet>();
                            }
                        }
                    });
                    setChoiceRenderer(new ChoiceRenderer<TagSet>()
                    {
                        @Override
                        public Object getDisplayValue(TagSet aObject)
                        {
                            return "[" + aObject.getType().getName() + "] " + aObject.getName();
                        }
                    });
                    setNullValid(false);
                }

                @Override
                protected void onSelectionChanged(TagSet aNewSelection)
                {
                    if (aNewSelection != null) {
                        // TagSetSelectionForm.this.getModelObject().tagSet = new TagSet();
                        tagSetDetailForm.clearInput();
                        tagSetDetailForm.setModelObject(aNewSelection);
                        tagSetDetailForm.setVisible(true);
                        TagSetSelectionForm.this.setVisible(true);
                        tagSelectionForm.setVisible(true);
                        tagDetailForm.setVisible(true);

                    }
                }

                @Override
                protected boolean wantOnSelectionChangedNotifications()
                {
                    return true;
                }

                @Override
                protected CharSequence getDefaultChoice(String aSelectedValue)
                {
                    return aSelectedValue;
                }
            });

            add(fileUpload = new FileUploadField("content", new Model()));
            add(new Button("import", new ResourceModel("label"))
            {

                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    uploadedFiles = fileUpload.getFileUploads();
                    Project project = selectedProjectModel.getObject();
                    String username = SecurityContextHolder.getContext().getAuthentication()
                            .getName();
                    User user = projectRepository.getUser(username);

                    if (isNotEmpty(uploadedFiles)) {
                        for (FileUpload tagFile : uploadedFiles) {
                            InputStream tagInputStream;
                            try {
                                tagInputStream = tagFile.getInputStream();
                                String text = IOUtils.toString(tagInputStream, "UTF-8");

                                MappingJacksonHttpMessageConverter jsonConverter = new MappingJacksonHttpMessageConverter();
                                ExportedTagSets importedTagSet = jsonConverter.getObjectMapper()
                                        .readValue(text, ExportedTagSets.class);
                                List<ExportedTagSetContent> importedTagSets = importedTagSet
                                        .getTagSets();

                                AnnotationType type = null;
                                List<AnnotationType> types = annotationService.getTypes(
                                        importedTagSets.get(0).getTypeName(), importedTagSets
                                                .get(0).getType());
                                if (types.size() == 0) {
                                    type = new AnnotationType();
                                    type.setDescription(importedTagSets.get(0).getTypeDescription());
                                    type.setName(importedTagSets.get(0).getTypeName());
                                    type.setType(importedTagSets.get(0).getType());
                                    annotationService.createType(type);
                                }
                                else {
                                    type = types.get(0);
                                }

                                for (ExportedTagSetContent tagSet : importedTagSets) {
                                    List<TagSet> tagSetInDb = annotationService.getTagSet(type,
                                            project);
                                    if (tagSetInDb.size() == 0) {
                                        TagSet newTagSet = new TagSet();
                                        newTagSet.setDescription(tagSet.getDescription());
                                        newTagSet.setName(tagSet.getName());
                                        newTagSet.setLanguage(tagSet.getLanguage());
                                        newTagSet.setProject(project);
                                        newTagSet.setType(type);
                                        annotationService.createTagSet(newTagSet, user);
                                        for (de.tudarmstadt.ukp.clarin.webanno.export.model.Tag tag : tagSet
                                                .getTags()) {
                                            Tag newTag = new Tag();
                                            newTag.setDescription(tag.getDescription());
                                            newTag.setName(tag.getName());
                                            newTag.setTagSet(newTagSet);
                                            annotationService.createTag(newTag, user);
                                        }
                                        info("TagSet successfully imported. Refresh page to see the imported TagSet.");
                                    }
                                    else {
                                        error("Tagset" + importedTagSets.get(0).getName()
                                                + " already exist");
                                    }
                                }

                            }
                            catch (IOException e) {
                                error("Error Importing TagSDet "
                                        + ExceptionUtils.getRootCauseMessage(e));
                            }
                        }
                    }
                    else if (isEmpty(uploadedFiles)) {
                        error("No Tagset File is selected to upload, please select a document first");
                    }
                    else if (project.getId() == 0) {
                        error("Project not yet created, please save project Details!");
                    }

                }
            });

        }
    }

    private class SelectionModel
        implements Serializable
    {
        private static final long serialVersionUID = -1L;

        private TagSet tagSet;
        private Tag tag;
    }

    private class TagSetDetailForm
        extends Form<TagSet>
    {
        private static final long serialVersionUID = -1L;
        TagSet tagSet;
        private List<FileUpload> uploadedFiles;
        private FileUploadField fileUpload;

        public TagSetDetailForm(String id)
        {
            super(id, new CompoundPropertyModel<TagSet>(new EntityModel<TagSet>(new TagSet())));
            // super(id);
            add(new TextField<String>("name").setRequired(true));

            add(new TextArea<String>("description").setOutputMarkupPlaceholderTag(true));

            add(new TextField<String>("language"));

            add(new DropDownChoice<AnnotationType>("type", annotationService.listAnnotationType(),
                    new ChoiceRenderer<AnnotationType>("name")).setRequired(true));

            add(new Button("save", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    TagSet tagSet = TagSetDetailForm.this.getModelObject();

                    if (tagSet.getId() == 0) {
                        try {
                            annotationService.getTagSet(tagSet.getType(),
                                    selectedProjectModel.getObject());
                            error("Only one tagset per type per project is allowed!");
                        }
                        catch (NoResultException ex) {

                            String username = SecurityContextHolder.getContext()
                                    .getAuthentication().getName();
                            User user = projectRepository.getUser(username);

                            tagSet.setProject(selectedProjectModel.getObject());
                            try {
                                annotationService.createTagSet(tagSet, user);
                            }
                            catch (IOException e) {
                                error("unable to create Log file while creating the TagSet" + ":"
                                        + ExceptionUtils.getRootCauseMessage(e));
                            }
                            TagSetDetailForm.this.setModelObject(tagSet);
                            tagSelectionForm.setVisible(true);
                            tagDetailForm.setVisible(true);
                        }
                    }
                }
            });

            add(new Button("remove", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    TagSet tagSet = TagSetDetailForm.this.getModelObject();
                    if (tagSet.getId() != 0) {
                        annotationService.removeTagSet(tagSet);
                        TagSetDetailForm.this.setModelObject(null);
                        tagSelectionForm.setVisible(false);
                        tagDetailForm.setVisible(false);
                    }
                    TagSetDetailForm.this.setModelObject(new TagSet());
                }
            });

            add(fileUpload = new FileUploadField("content", new Model()));
            add(new Button("import", new ResourceModel("label"))
            {

                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    uploadedFiles = fileUpload.getFileUploads();
                    Project project = selectedProjectModel.getObject();

                    if (isNotEmpty(uploadedFiles)) {
                        for (FileUpload tagFile : uploadedFiles) {
                            InputStream tagInputStream;
                            try {
                                TagSet tagSet = TagSetDetailForm.this.getModelObject();
                                if (tagSet.getId() != 0) {
                                    tagInputStream = tagFile.getInputStream();
                                    String text = IOUtils.toString(tagInputStream, "UTF-8");
                                    Map<String, String> mapOfTagsAndDescriptionsFromFile = ApplicationUtils
                                            .getTagsFromText(text);

                                    List<Tag> listOfTagsFromDatabase = annotationService
                                            .listTags(tagSet);
                                    Set<String> listOfTagsFromFile = mapOfTagsAndDescriptionsFromFile
                                            .keySet();

                                    Set<String> listOfTagNamesFromDatabse = new HashSet<String>();

                                    for (Tag tag : listOfTagsFromDatabase) {
                                        listOfTagNamesFromDatabse.add(tag.getName());
                                    }

                                    listOfTagsFromFile.removeAll(listOfTagNamesFromDatabse);

                                    for (String tagName : listOfTagsFromFile) {

                                        String username = SecurityContextHolder.getContext()
                                                .getAuthentication().getName();
                                        User user = projectRepository.getUser(username);

                                        Tag tag = new Tag();
                                        tag.setDescription(mapOfTagsAndDescriptionsFromFile
                                                .get(tagName));
                                        tag.setName(tagName);
                                        tag.setTagSet(tagSet);
                                        annotationService.createTag(tag, user);
                                    }
                                }
                                else {
                                    error("Please save TagSet first!");
                                }
                            }
                            catch (Exception e) {
                                error("Error uploading document "
                                        + ExceptionUtils.getRootCauseMessage(e));
                            }
                        }
                    }
                    else if (isEmpty(uploadedFiles)) {
                        error("No document is selected to upload, please select a document first");
                    }
                    else if (project.getId() == 0) {
                        error("Project not yet created, please save project Details!");
                    }

                }
            });

            add(new DownloadLink("export", new LoadableDetachableModel<File>()
            {
                private static final long serialVersionUID = 840863954694163375L;

                @Override
                protected File load()
                {
                    File downloadFile = null;
                    try {
                        downloadFile = File.createTempFile("exportedtagsets", ".json");
                    }
                    catch (IOException e1) {
                        error("Unable to create temporary File!!");

                    }
                    if (selectedProjectModel.getObject().getId() == 0) {
                        error("Project not yet created. Please save project details first!");
                    }
                    else {
                        List<ExportedTagSetContent> exportedTagSetscontent = new ArrayList<ExportedTagSetContent>();
                        TagSet tagSet = tagSetDetailForm.getModelObject();
                        ExportedTagSetContent exportedTagSetContent = new ExportedTagSetContent();
                        exportedTagSetContent.setDescription(tagSet.getDescription());
                        exportedTagSetContent.setLanguage(tagSet.getLanguage());
                        exportedTagSetContent.setName(tagSet.getName());

                        exportedTagSetContent.setType(tagSet.getType().getType());
                        exportedTagSetContent.setTypeName(tagSet.getType().getName());
                        exportedTagSetContent.setTypeDescription(tagSet.getType().getDescription());

                        List<de.tudarmstadt.ukp.clarin.webanno.export.model.Tag> exportedTags = new ArrayList<de.tudarmstadt.ukp.clarin.webanno.export.model.Tag>();
                        for (Tag tag : annotationService.listTags(tagSet)) {
                            de.tudarmstadt.ukp.clarin.webanno.export.model.Tag exportedTag = new de.tudarmstadt.ukp.clarin.webanno.export.model.Tag();
                            exportedTag.setDescription(tag.getDescription());
                            exportedTag.setName(tag.getName());
                            exportedTags.add(exportedTag);

                        }
                        exportedTagSetContent.setTags(exportedTags);
                        exportedTagSetscontent.add(exportedTagSetContent);
                        ExportedTagSets exportedTagSet = new ExportedTagSets();
                        exportedTagSet.setTagSets(exportedTagSetscontent);
                        CasToBratJson exportedTagSets = new CasToBratJson();
                        MappingJacksonHttpMessageConverter jsonConverter = new MappingJacksonHttpMessageConverter();
                        exportedTagSets.setJsonConverter(jsonConverter);

                        try {
                            exportedTagSets.generateBratJson(exportedTagSet, downloadFile);
                        }
                        catch (IOException e) {
                            error("File Path not found or No permision to save the file!");
                        }
                        info("TagSets successfully exported to :" + downloadFile.getAbsolutePath());

                    }
                    return downloadFile;
                }
            }).setOutputMarkupId(true));

        }
    }

    private class TagDetailForm
        extends Form<Tag>
    {
        private static final long serialVersionUID = -1L;

        public TagDetailForm(String id)
        {
            super(id, new CompoundPropertyModel<Tag>(new EntityModel<Tag>(new Tag())));
            // super(id);
            add(new TextField<String>("name").setRequired(true));

            add(new TextArea<String>("description").setOutputMarkupPlaceholderTag(true));
            add(new Button("save", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    Tag tag = TagDetailForm.this.getModelObject();
                    if (tag.getId() == 0) {
                        tag.setTagSet(tagSetDetailForm.getModelObject());
                        try {
                            annotationService.getTag(tag.getName(),
                                    tagSetDetailForm.getModelObject());
                            error("This tag is already added for this tagset!");
                        }
                        catch (NoResultException ex) {

                            String username = SecurityContextHolder.getContext()
                                    .getAuthentication().getName();
                            User user = projectRepository.getUser(username);

                            try {
                                annotationService.createTag(tag, user);
                            }
                            catch (IOException e) {
                                error("unable to create a log file while creating the Tag " + ":"
                                        + ExceptionUtils.getRootCauseMessage(e));
                            }
                            tagDetailForm.setModelObject(new Tag());
                        }
                    }
                }
            });

            add(new Button("new", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    TagDetailForm.this.setDefaultModelObject(new Tag());
                }
            });

            add(new Button("remove", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    Tag tag = TagDetailForm.this.getModelObject();
                    if (tag.getId() != 0) {
                        tag.setTagSet(tagSetDetailForm.getModelObject());
                        annotationService.removeTag(tag);
                        tagDetailForm.setModelObject(new Tag());
                    }
                    else {
                        TagDetailForm.this.setModelObject(new Tag());
                    }
                }
            });
        }
    }

    private class TagSelectionForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -1L;
        private Tag selectedTag;
        private ListChoice<Tag> tags;

        public TagSelectionForm(String id)
        {
            // super(id);
            super(id, new CompoundPropertyModel<SelectionModel>(new SelectionModel()));

            add(tags = new ListChoice<Tag>("tag")
            {
                private static final long serialVersionUID = 1L;

                {
                    setChoices(new LoadableDetachableModel<List<Tag>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<Tag> load()
                        {
                            return annotationService.listTags(tagSetDetailForm.getModelObject());
                        }
                    });
                    setChoiceRenderer(new ChoiceRenderer<Tag>("name", "id"));
                    setNullValid(false);

                }

                @Override
                protected CharSequence getDefaultChoice(String aSelectedValue)
                {
                    return "";
                }
            });
            tags.add(new OnChangeAjaxBehavior()
            {
                private static final long serialVersionUID = 7492425689121761943L;

                @Override
                protected void onUpdate(AjaxRequestTarget aTarget)
                {
                    tagDetailForm.setModelObject(getModelObject().tag);
                    aTarget.add(tagDetailForm.setOutputMarkupId(true));
                }
            }).setOutputMarkupId(true);
        }
    }

    private TagSetSelectionForm tagSetSelectionForm;
    private TagSelectionForm tagSelectionForm;
    private TagSetDetailForm tagSetDetailForm;
    private TagDetailForm tagDetailForm;

    private List<FileUpload> uploadedFiles;
    private FileUploadField fileUpload;

    private Model<Project> selectedProjectModel;

    public ProjectTagSetsPanel(String id, Model<Project> aProjectModel)
    {
        super(id);
        this.selectedProjectModel = aProjectModel;
        tagSetSelectionForm = new TagSetSelectionForm("tagSetSelectionForm");

        tagSelectionForm = new TagSelectionForm("tagSelectionForm");
        tagSelectionForm.setVisible(false);

        tagSetDetailForm = new TagSetDetailForm("tagSetDetailForm");
        tagSetDetailForm.setVisible(false);

        tagDetailForm = new TagDetailForm("tagDetailForm");
        tagDetailForm.setVisible(false);

        add(tagSetSelectionForm);
        add(tagSelectionForm);
        add(tagSetDetailForm);
        add(tagDetailForm);
    }
}