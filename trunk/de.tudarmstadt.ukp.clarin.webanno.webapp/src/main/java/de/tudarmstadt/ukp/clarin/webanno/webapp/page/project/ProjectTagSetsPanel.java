/*******************************************************************************
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
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
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedTagSetConstant;
import de.tudarmstadt.ukp.clarin.webanno.export.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationType;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.webapp.support.EntityModel;

/**
 * A Panel Used to add Tagsets to a selected {@link Project}
 *
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

    private List<FileUpload> uploadedFiles;
    private FileUploadField fileUpload;
    DropDownChoice<String> importTagsetFormat;
    DropDownChoice<String> exportTagsetFormat;
    private String selectedExporTagsetFormat = ExportedTagSetConstant.JSON_FORMAT;

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
                        tagSetDetailForm
                                .setModelObject(new de.tudarmstadt.ukp.clarin.webanno.model.TagSet());
                        tagSetDetailForm.setVisible(true);
                        tagSelectionForm.setVisible(false);
                        tagDetailForm.setVisible(false);
                    }
                }
            });

            add(new ListChoice<de.tudarmstadt.ukp.clarin.webanno.model.TagSet>("tagSet")
            {
                private static final long serialVersionUID = 1L;

                {
                    setChoices(new LoadableDetachableModel<List<de.tudarmstadt.ukp.clarin.webanno.model.TagSet>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<de.tudarmstadt.ukp.clarin.webanno.model.TagSet> load()
                        {
                            Project project = selectedProjectModel.getObject();
                            if (project.getId() != 0) {
                                return annotationService.listTagSets(project);
                            }
                            else {
                                return new ArrayList<de.tudarmstadt.ukp.clarin.webanno.model.TagSet>();
                            }
                        }
                    });
                    setChoiceRenderer(new ChoiceRenderer<de.tudarmstadt.ukp.clarin.webanno.model.TagSet>()
                    {
                        @Override
                        public Object getDisplayValue(
                                de.tudarmstadt.ukp.clarin.webanno.model.TagSet aObject)
                        {
                            return "[" + aObject.getType().getName() + "] " + aObject.getName();
                        }
                    });
                    setNullValid(false);
                }

                @Override
                protected void onSelectionChanged(
                        de.tudarmstadt.ukp.clarin.webanno.model.TagSet aNewSelection)
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
            }).setOutputMarkupId(true);

        }
    }

    private class ImportTagSetForm
        extends Form<String>
    {

        private static final long serialVersionUID = 5286655225171641733L;

        public ImportTagSetForm(String id)
        {
            super(id);
            add(fileUpload = new FileUploadField("content", new Model()));
            add(importTagsetFormat = new DropDownChoice<String>("importTagsetFormat",
                    new Model<String>(selectedExporTagsetFormat),
                    Arrays.asList(new String[] { ExportedTagSetConstant.JSON_FORMAT,
                            ExportedTagSetConstant.TAB_FORMAT })));
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

                    de.tudarmstadt.ukp.clarin.webanno.model.TagSet selectionModelTagSet = null;
                    if (isNotEmpty(uploadedFiles)) {
                        if (importTagsetFormat.getModelObject().equals(
                                ExportedTagSetConstant.JSON_FORMAT)) {
                            for (FileUpload tagFile : uploadedFiles) {
                                InputStream tagInputStream;
                                try {
                                    tagInputStream = tagFile.getInputStream();
                                    String text = IOUtils.toString(tagInputStream, "UTF-8");

                                    MappingJacksonHttpMessageConverter jsonConverter = new MappingJacksonHttpMessageConverter();
                                    TagSet importedTagSet = jsonConverter.getObjectMapper()
                                            .readValue(text, TagSet.class);
                                    AnnotationType type = null;
                                    if (!annotationService.existsType(importedTagSet.getTypeName(),
                                            importedTagSet.getType())) {
                                        type = new AnnotationType();
                                        type.setDescription(importedTagSet.getTypeDescription());
                                        type.setName(importedTagSet.getTypeName());
                                        type.setType(importedTagSet.getType());
                                        annotationService.createType(type);
                                    }
                                    else {
                                        type = annotationService.getType(
                                                importedTagSet.getTypeName(),
                                                importedTagSet.getType());
                                    }

                                    if (importedTagSet != null) {

                                        // Override existing tagset
                                        if (annotationService.existTagSet(type, project)) {
                                            annotationService.removeTagSet(annotationService
                                                    .getTagSet(type, project));
                                        }
                                        de.tudarmstadt.ukp.clarin.webanno.model.TagSet newTagSet = new de.tudarmstadt.ukp.clarin.webanno.model.TagSet();
                                        newTagSet.setDescription(importedTagSet.getDescription());
                                        newTagSet.setName(importedTagSet.getName());
                                        newTagSet.setLanguage(importedTagSet.getLanguage());
                                        newTagSet.setProject(project);
                                        newTagSet.setType(type);
                                        annotationService.createTagSet(newTagSet, user);
                                        for (de.tudarmstadt.ukp.clarin.webanno.export.model.Tag tag : importedTagSet
                                                .getTags()) {
                                            Tag newTag = new Tag();
                                            newTag.setDescription(tag.getDescription());
                                            newTag.setName(tag.getName());
                                            newTag.setTagSet(newTagSet);
                                            annotationService.createTag(newTag, user);
                                        }
                                        selectionModelTagSet = newTagSet;
                                    }

                                }
                                catch (IOException e) {
                                    error("Error Importing TagSet "
                                            + ExceptionUtils.getRootCauseMessage(e));
                                }
                            }

                        }
                        else if (importTagsetFormat.getModelObject().equals(
                                ExportedTagSetConstant.TAB_FORMAT)) {
                            for (FileUpload tagFile : uploadedFiles) {
                                InputStream tagInputStream;
                                try {
                                    tagInputStream = tagFile.getInputStream();
                                    String text = IOUtils.toString(tagInputStream, "UTF-8");
                                    Map<String, String> tabbedTagsetFromFile = ApplicationUtils
                                            .getTagSetFromFile(text);

                                    Set<String> listOfTagsFromFile = tabbedTagsetFromFile.keySet();
                                    int i = 0;
                                    String tagSetName = "";
                                    String tagSetDescription = "";
                                    String tagsetLanguage = "";
                                    String tagsetType = "";
                                    String tagsetTypeName = "";
                                    String tagsetTypeDescription = "";
                                    de.tudarmstadt.ukp.clarin.webanno.model.TagSet tagSet = null;
                                    for (String key : listOfTagsFromFile) {
                                        // the first key is the tagset name and its description
                                        if (i == 0) {
                                            tagSetName = key;
                                            tagSetDescription = tabbedTagsetFromFile.get(key);
                                        }
                                        // the second key is the tagset type
                                        else if (i == 1) {
                                            tagsetType = key;
                                        }
                                        // the third key is the tagset type name and its description
                                        else if (i == 2) {
                                            tagsetTypeName = key;
                                            tagsetTypeDescription = tabbedTagsetFromFile.get(key);
                                        }
                                        // the fourth key is the tagset language
                                        else if (i == 3) {
                                            tagsetLanguage = key;
                                            // Create the type, if not exist
                                            AnnotationType type = null;
                                            if (!annotationService.existsType(tagsetTypeName,
                                                    tagsetType)) {
                                                type = new AnnotationType();
                                                type.setDescription(tagsetTypeDescription.replace(
                                                        "\\n", "\n"));
                                                type.setName(tagsetTypeName);
                                                type.setType(tagsetType);
                                                annotationService.createType(type);
                                            }
                                            // type exist, get it
                                            else {
                                                type = annotationService.getType(tagsetTypeName,
                                                        tagsetType);
                                            }
                                            // remove and replace the tagset if it exist
                                            if (annotationService.existTagSet(type, project)) {
                                                annotationService.removeTagSet(annotationService
                                                        .getTagSet(type, project));
                                            }
                                            tagSet = new de.tudarmstadt.ukp.clarin.webanno.model.TagSet();
                                            tagSet.setName(tagSetName);
                                            tagSet.setDescription(tagSetDescription.replace("\\n",
                                                    "\n"));
                                            tagSet.setLanguage(tagsetLanguage);
                                            tagSet.setProject(project);
                                            tagSet.setType(type);
                                            annotationService.createTagSet(tagSet, user);
                                        }
                                        // otherwise it is a tag entry, add the tag to the tagset
                                        else {
                                            Tag tag = new Tag();
                                            tag.setDescription(tabbedTagsetFromFile.get(key)
                                                    .replace("\\n", "\n"));
                                            tag.setName(key);
                                            tag.setTagSet(tagSet);
                                            annotationService.createTag(tag, user);
                                        }
                                        i++;
                                    }
                                }
                                catch (Exception e) {
                                    error("Error Importing tabbed TagSet."
                                            + ExceptionUtils.getRootCauseMessage(e));
                                }
                            }
                        }
                        tagSetSelectionForm.setModelObject(new SelectionModel());
                        tagSelectionForm.setVisible(false);
                        tagDetailForm.setVisible(false);
                    }
                    else if (isEmpty(uploadedFiles)) {
                        error("Please choose file with tagset before uploading");
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

        private de.tudarmstadt.ukp.clarin.webanno.model.TagSet tagSet;
        private Tag tag;
    }

    private class TagSetDetailForm
        extends Form<de.tudarmstadt.ukp.clarin.webanno.model.TagSet>
    {
        private static final long serialVersionUID = -1L;
        TagSet tagSet;
        private List<FileUpload> uploadedFiles;
        private FileUploadField fileUpload;

        @SuppressWarnings("unchecked")
        public TagSetDetailForm(String id)
        {
            super(id, new CompoundPropertyModel<de.tudarmstadt.ukp.clarin.webanno.model.TagSet>(
                    new EntityModel<de.tudarmstadt.ukp.clarin.webanno.model.TagSet>(
                            new de.tudarmstadt.ukp.clarin.webanno.model.TagSet())));
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
                    de.tudarmstadt.ukp.clarin.webanno.model.TagSet tagSet = TagSetDetailForm.this
                            .getModelObject();

                    if (tagSet.getId() == 0) {
                        if (annotationService.existTagSet(tagSet.getType(),
                                selectedProjectModel.getObject())) {
                            error("Only one tagset per type per project is allowed!");
                        }
                        else {

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
                    de.tudarmstadt.ukp.clarin.webanno.model.TagSet tagSet = TagSetDetailForm.this
                            .getModelObject();
                    if (tagSet.getId() != 0) {
                        annotationService.removeTagSet(tagSet);
                        TagSetDetailForm.this.setModelObject(null);
                        tagSelectionForm.setVisible(false);
                        tagDetailForm.setVisible(false);
                        TagSetDetailForm.this.setVisible(false);
                    }
                    TagSetDetailForm.this
                            .setModelObject(new de.tudarmstadt.ukp.clarin.webanno.model.TagSet());
                }
            });

            add(exportTagsetFormat = (DropDownChoice<String>) new DropDownChoice<String>(
                    "exportTagsetFormat", new Model<String>(selectedExporTagsetFormat),
                    Arrays.asList(new String[] { ExportedTagSetConstant.JSON_FORMAT,
                            ExportedTagSetConstant.TAB_FORMAT })).add(new OnChangeAjaxBehavior()
            {

                @Override
                protected void onUpdate(AjaxRequestTarget target)
                {
                    selectedExporTagsetFormat = exportTagsetFormat.getModelObject();

                }
            }));

            add(new DownloadLink("export", new LoadableDetachableModel<File>()
            {
                private static final long serialVersionUID = 840863954694163375L;

                @Override
                protected File load()
                {
                    File exportFile = null;
                    if (selectedExporTagsetFormat.equals(ExportedTagSetConstant.JSON_FORMAT)) {
                        try {
                            exportFile = File.createTempFile("exportedtagsets", ".json");
                        }
                        catch (IOException e1) {
                            error("Unable to create temporary File!!");

                        }
                        if (selectedProjectModel.getObject().getId() == 0) {
                            error("Project not yet created. Please save project details first!");
                        }
                        else {
                            de.tudarmstadt.ukp.clarin.webanno.model.TagSet tagSet = tagSetDetailForm
                                    .getModelObject();
                            TagSet exportedTagSetContent = new TagSet();
                            exportedTagSetContent.setDescription(tagSet.getDescription());
                            exportedTagSetContent.setLanguage(tagSet.getLanguage());
                            exportedTagSetContent.setName(tagSet.getName());

                            exportedTagSetContent.setType(tagSet.getType().getType());
                            exportedTagSetContent.setTypeName(tagSet.getType().getName());
                            exportedTagSetContent.setTypeDescription(tagSet.getType()
                                    .getDescription());

                            List<de.tudarmstadt.ukp.clarin.webanno.export.model.Tag> exportedTags = new ArrayList<de.tudarmstadt.ukp.clarin.webanno.export.model.Tag>();
                            for (Tag tag : annotationService.listTags(tagSet)) {
                                de.tudarmstadt.ukp.clarin.webanno.export.model.Tag exportedTag = new de.tudarmstadt.ukp.clarin.webanno.export.model.Tag();
                                exportedTag.setDescription(tag.getDescription());
                                exportedTag.setName(tag.getName());
                                exportedTags.add(exportedTag);

                            }
                            exportedTagSetContent.setTags(exportedTags);
                            MappingJacksonHttpMessageConverter jsonConverter = new MappingJacksonHttpMessageConverter();
                            ApplicationUtils.setJsonConverter(jsonConverter);

                            try {
                                ApplicationUtils.generateJson(exportedTagSetContent, exportFile);
                            }
                            catch (IOException e) {
                                error("File Path not found or No permision to save the file!");
                            }
                            info("TagSets successfully exported to :"
                                    + exportFile.getAbsolutePath());

                        }
                    }
                    else if (selectedExporTagsetFormat.equals(ExportedTagSetConstant.TAB_FORMAT)) {
                        de.tudarmstadt.ukp.clarin.webanno.model.TagSet tagSet = tagSetDetailForm
                                .getModelObject();
                        try {
                            exportFile = File.createTempFile("exportedtagsets", ".txt");
                        }
                        catch (IOException e1) {
                            error("Unable to create temporary File!!");
                        }
                        OutputStream os;
                        OutputStreamWriter osw;
                        BufferedWriter bw;
                        try {
                            String typeDescription = tagSet.getType().getDescription() == null ? ""
                                    : tagSet.getType().getDescription();
                            String tagSetDescription = tagSet.getDescription() == null ? ""
                                    : tagSet.getDescription();
                            os = (OutputStream) new FileOutputStream(exportFile);
                            osw = new OutputStreamWriter(os, "UTF-8");
                            bw = new BufferedWriter(osw);
                            bw.write(tagSet.getName() + "\t"
                                    + tagSetDescription.replace("\n", "\\n") + "\n");
                            bw.write(tagSet.getType().getType() + "\t" + " \n");
                            bw.write(tagSet.getType().getName() + "\t"
                                    + typeDescription.replace("\n", "\\n") + "\n");
                            bw.write(tagSet.getLanguage() + "\t" + " \n");
                            for (Tag tag : annotationService.listTags(tagSet)) {
                                String tagDescription = tag.getDescription() == null ? "" : tag
                                        .getDescription();
                                bw.write(tag.getName() + "\t"
                                        + tagDescription.replace("\n", "\\n") + "\n");
                            }

                            bw.flush();
                            bw.close();
                        }
                        catch (FileNotFoundException e) {
                            error("The file for export not found "
                                    + ExceptionUtils.getRootCauseMessage(e));
                        }
                        catch (UnsupportedEncodingException e) {
                            error("Unsupported encoding " + ExceptionUtils.getRootCauseMessage(e));
                        }
                        catch (IOException e) {
                            error(ExceptionUtils.getRootCause(e));
                        }

                    }
                    return exportFile;
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
                    setChoiceRenderer(new ChoiceRenderer<Tag>()
                    {
                        private static final long serialVersionUID = 4696303692557735150L;

                        @Override
                        public Object getDisplayValue(Tag aObject)
                        {
                            return aObject.getName();
                        }
                    });
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
                    if (getModelObject().tag != null) {
                        tagDetailForm.setModelObject(getModelObject().tag);
                        aTarget.add(tagDetailForm.setOutputMarkupId(true));
                    }
                }
            }).setOutputMarkupId(true);

            add(new Button("new", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    tagDetailForm.setDefaultModelObject(new Tag());
                }
            });
        }
    }

    private TagSetSelectionForm tagSetSelectionForm;
    private TagSelectionForm tagSelectionForm;
    private TagSetDetailForm tagSetDetailForm;
    private TagDetailForm tagDetailForm;
    private ImportTagSetForm importTagSetForm;

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

        importTagSetForm = new ImportTagSetForm("importTagSetForm");

        add(tagSetSelectionForm);
        add(tagSelectionForm);
        add(tagSetDetailForm);
        add(tagDetailForm);
        add(importTagSetForm);
    }
}
