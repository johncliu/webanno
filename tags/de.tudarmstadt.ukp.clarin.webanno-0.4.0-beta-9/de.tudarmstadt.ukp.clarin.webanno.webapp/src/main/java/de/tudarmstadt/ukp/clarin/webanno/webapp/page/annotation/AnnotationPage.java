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
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.annotation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.NoResultException;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.link.DownloadLink;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.security.core.context.SecurityContextHolder;

import wicket.contrib.input.events.EventType;
import wicket.contrib.input.events.InputBehavior;
import wicket.contrib.input.events.key.KeyType;
import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.ApplicationUtils;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.AnnotationPreference;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotator;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasController;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.webapp.dialog.OpenDocumentModel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.dialog.OpenModalWindowPanel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.ApplicationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.annotation.component.AnnotationLayersModalPanel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.annotation.component.DocumentNamePanel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.annotation.component.ExportModalPanel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.annotation.component.FinishImage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.annotation.component.FinishLink;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.annotation.component.GuidelineModalPanel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.welcome.WelcomePage;

/**
 * A wicket page for the Brat Annotation/Visualization page. Included components for pagination,
 * annotation layer configuration, and Exporting document
 *
 * @author Seid Muhie Yimam
 *
 */
public class AnnotationPage
    extends ApplicationPageBase
{
    private static final long serialVersionUID = 1378872465851908515L;

    private BratAnnotator annotator;

    private OpenDocumentModel openDataModel;
    @SpringBean(name = "jsonConverter")
    private MappingJacksonHttpMessageConverter jsonConverter;

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    private DownloadLink export;
    FinishImage finish;
    private int windowSize;

    private NumberTextField<Integer> gotoPageTextField;
    private int gotoPageAddress = -1;

    // Open the dialog window on first load
    boolean firstLoad = true;

    private Label numberOfPages;
    private DocumentNamePanel documentNamePanel;

    private long currentDocumentId;
    private long currentprojectId;

    private int sentenceNumber = 1;
    private int totalNumberOfSentence;

    public BratAnnotatorModel bratAnnotatorModel = new BratAnnotatorModel();

    @SuppressWarnings("deprecation")
    public AnnotationPage()
    {

        annotator = new BratAnnotator("embedder1",
                new Model<BratAnnotatorModel>(bratAnnotatorModel))
        {

            private static final long serialVersionUID = 7279648231521710155L;

            @Override
            protected void onChange(AjaxRequestTarget aTarget)
            {
                // updateRightSide(aTarget, sentenceOuterView, curationContainer, this);
                aTarget.add(numberOfPages);
            }
        };
        annotator.setOutputMarkupId(true);

        // This is an Annotation Operation, set model to ANNOTATION mode
        bratAnnotatorModel.setMode(Mode.ANNOTATION);
        add(annotator);

        add(documentNamePanel = new DocumentNamePanel("documentNamePanel",
                new Model<BratAnnotatorModel>(bratAnnotatorModel)));

        add(numberOfPages = (Label) new Label("numberOfPages",
                new LoadableDetachableModel<String>()
                {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected String load()
                    {
                        if (bratAnnotatorModel.getDocument() != null) {
                            try {
                                JCas jCas = getCas(bratAnnotatorModel.getProject(),
                                        bratAnnotatorModel.getUser(),
                                        bratAnnotatorModel.getDocument());
                                totalNumberOfSentence = BratAjaxCasUtil.getNumberOfPages(jCas,
                                        bratAnnotatorModel.getWindowSize());

                                // If only one page, start displaying from sentence 1
                                if (totalNumberOfSentence == 1) {
                                    bratAnnotatorModel.setSentenceAddress(bratAnnotatorModel
                                            .getFirstSentenceAddress());
                                }
                                sentenceNumber = BratAjaxCasUtil.getSentenceNumber(jCas,
                                        bratAnnotatorModel.getSentenceAddress());
                                int firstSentenceNumber = sentenceNumber + 1;
                                int lastSentenceNumber;
                                if (firstSentenceNumber + bratAnnotatorModel.getWindowSize() - 1 < totalNumberOfSentence) {
                                    lastSentenceNumber = firstSentenceNumber
                                            + bratAnnotatorModel.getWindowSize() - 1;
                                }
                                else {
                                    lastSentenceNumber = totalNumberOfSentence;
                                }

                                return "showing " + firstSentenceNumber + "-" + lastSentenceNumber
                                        + " of " + totalNumberOfSentence + " sentences";
                            }
                            // No need to report error, already reported in getDocument below
                            catch (DataRetrievalFailureException ex) {
                                // error(ExceptionUtils.getRootCauseMessage(ex));
                                return "";
                            }
                            // No need to report error, already reported in getDocument below
                            catch (UIMAException e) {
                                // error(ExceptionUtils.getRootCauseMessage(e));
                                return "";
                            }
                            catch (IOException e) {
                                return "";
                            }
                            catch (ClassNotFoundException e) {
                                return "";
                            }

                        }
                        else {
                            return "";// no document yet selected
                        }

                    }
                }).setOutputMarkupId(true));

        // Add a dialog panel to select annotation layers, window size and display lemma option
        openDataModel = new OpenDocumentModel();
        final ModalWindow openDocumentsModal;
        add(openDocumentsModal = new ModalWindow("openDocumentsModal"));
        openDocumentsModal.setOutputMarkupId(true);

        openDocumentsModal.setInitialWidth(500);
        openDocumentsModal.setInitialHeight(300);
        openDocumentsModal.setResizable(true);
        openDocumentsModal.setWidthUnit("px");
        openDocumentsModal.setHeightUnit("px");
        openDocumentsModal.setTitle("Open document");

        add(new AjaxLink<Void>("showOpenDocumentModal")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                openDocumentsModal.setContent(new OpenModalWindowPanel(openDocumentsModal
                        .getContentId(), openDataModel, openDocumentsModal, Mode.ANNOTATION));
                openDocumentsModal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback()
                {
                    private static final long serialVersionUID = -1746088901018629567L;

                    @Override
                    public void onClose(AjaxRequestTarget target)
                    {
                        if (openDataModel.getProject() != null
                                && openDataModel.getDocument() != null) {
                            bratAnnotatorModel.setDocument(openDataModel.getDocument());
                            bratAnnotatorModel.setProject(openDataModel.getProject());
                            try {
                                // setAttributesForGetCollection();
                                setAttributesForDocument();
                            }
                            catch (IOException e) {
                                error(e.getMessage());
                            }
                            catch (UIMAException e) {
                                error(ExceptionUtils.getRootCauseMessage(e));
                            }
                            catch (ClassNotFoundException e) {
                                error(e.getMessage());
                            }
                            String collection = "#" + openDataModel.getProject().getName() + "/";
                            String document = openDataModel.getDocument().getName();
                            target.add(finish.setOutputMarkupId(true));
                            target.appendJavaScript("window.location.hash = '"
                                    + collection
                                    + document
                                    + "';Wicket.Window.unloadConfirmation=false;window.location.reload()");

                        }
                        else {
                            // A hack, the dialog opens for the first time, and if no document is
                            // selected
                            // window will be "blind down". SOmething in the brat js causes this!
                            setResponsePage(WelcomePage.class);
                        }
                    }
                });
                // target.appendJavaScript("Wicket.Window.unloadConfirmation = false;");
                openDocumentsModal.show(target);
            }
        });

        add(new AnnotationLayersModalPanel("annotationLayersModalPanel",
                new Model<BratAnnotatorModel>(bratAnnotatorModel))
        {
            private static final long serialVersionUID = -4657965743173979437L;

            @Override
            protected void onChange(AjaxRequestTarget aTarget)
            {
                aTarget.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");
            }
        });

        add(new ExportModalPanel("exportModalPanel", new Model<BratAnnotatorModel>(
                bratAnnotatorModel)));
        // Show the previous document, if exist
        add(new AjaxLink<Void>("showPreviousDocument")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            /**
             * Get the current beginning sentence address and add on it the size of the display
             * window
             */
            @Override
            public void onClick(AjaxRequestTarget target)
            {
                // List of all Source Documents in the project
                List<SourceDocument> listOfSourceDocuements = repository
                        .listSourceDocuments(bratAnnotatorModel.getProject());

                String username = SecurityContextHolder.getContext().getAuthentication().getName();
                User user = repository.getUser(username);

                List<SourceDocument> sourceDocumentsinIgnorState = new ArrayList<SourceDocument>();
                for (SourceDocument sourceDocument : listOfSourceDocuements) {
                    if (repository.existsAnnotationDocument(sourceDocument, user)
                            && repository.getAnnotationDocument(sourceDocument, user).getState()
                                    .equals(AnnotationDocumentState.IGNORE)) {
                        sourceDocumentsinIgnorState.add(sourceDocument);
                    }
                }

                listOfSourceDocuements.removeAll(sourceDocumentsinIgnorState);

                // Index of the current source document in the list
                int currentDocumentIndex = listOfSourceDocuements.indexOf(bratAnnotatorModel
                        .getDocument());

                // If the first the document
                if (currentDocumentIndex == 0) {
                    target.appendJavaScript("alert('This is the first document!')");
                }
                else {
                    bratAnnotatorModel.setDocumentName(listOfSourceDocuements.get(
                            currentDocumentIndex - 1).getName());
                    bratAnnotatorModel.setDocument(listOfSourceDocuements
                            .get(currentDocumentIndex - 1));
                    try {
                        // setAttributesForGetCollection();
                        setAttributesForDocument();
                    }
                    catch (IOException e) {
                        error(e.getMessage());
                    }
                    catch (UIMAException e) {
                        error(ExceptionUtils.getRootCauseMessage(e));
                    }
                    catch (ClassNotFoundException e) {
                        error(e.getMessage());
                    }
                    String project = "#" + bratAnnotatorModel.getProject().getName() + "/";
                    String document = listOfSourceDocuements.get(currentDocumentIndex - 1)
                            .getName();
                    String rewriteUrl = project + document;
                    target.add(finish.setOutputMarkupId(true));
                    target.appendJavaScript("window.location.hash = '" + rewriteUrl
                            + "'; Wicket.Window.unloadConfirmation=false;window.location.reload()");
                }
            }
        }.add(new InputBehavior(new KeyType[] { KeyType.Shift, KeyType.Page_up }, EventType.click)));

        // Show the next document if exist
        add(new AjaxLink<Void>("showNextDocument")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            /**
             * Get the current beginning sentence address and add on it the size of the display
             * window
             */
            @Override
            public void onClick(AjaxRequestTarget target)
            {
                // List of all Source Documents in the project
                List<SourceDocument> listOfSourceDocuements = repository
                        .listSourceDocuments(bratAnnotatorModel.getProject());

                String username = SecurityContextHolder.getContext().getAuthentication().getName();
                User user = repository.getUser(username);

                List<SourceDocument> sourceDocumentsinIgnorState = new ArrayList<SourceDocument>();
                for (SourceDocument sourceDocument : listOfSourceDocuements) {
                    if (repository.existsAnnotationDocument(sourceDocument, user)
                            && repository.getAnnotationDocument(sourceDocument, user).getState()
                                    .equals(AnnotationDocumentState.IGNORE)) {
                        sourceDocumentsinIgnorState.add(sourceDocument);
                    }
                }

                listOfSourceDocuements.removeAll(sourceDocumentsinIgnorState);

                // Index of the current source document in the list
                int currentDocumentIndex = listOfSourceDocuements.indexOf(bratAnnotatorModel
                        .getDocument());

                // If the first document
                if (currentDocumentIndex == listOfSourceDocuements.size() - 1) {
                    target.appendJavaScript("alert('This is the last document!')");
                }
                else {
                    bratAnnotatorModel.setDocumentName(listOfSourceDocuements.get(
                            currentDocumentIndex + 1).getName());
                    bratAnnotatorModel.setDocument(listOfSourceDocuements
                            .get(currentDocumentIndex + 1));
                    try {
                        // setAttributesForGetCollection();
                        setAttributesForDocument();
                    }
                    catch (IOException e) {
                        error(e.getMessage());
                    }
                    catch (UIMAException e) {
                        error(ExceptionUtils.getRootCauseMessage(e));
                    }
                    catch (ClassNotFoundException e) {
                        error(e.getMessage());
                    }
                    ;
                    String project = "#" + bratAnnotatorModel.getProject().getName() + "/";
                    String document = listOfSourceDocuements.get(currentDocumentIndex + 1)
                            .getName();
                    String rewriteUrl = project + document;
                    target.add(finish.setOutputMarkupId(true));
                    target.appendJavaScript("window.location.hash = '" + rewriteUrl
                            + "'; Wicket.Window.unloadConfirmation=false;window.location.reload()");
                }
            }
        }.add(new InputBehavior(new KeyType[] { KeyType.Shift, KeyType.Page_down }, EventType.click)));

        // Show the next page of this document
        add(new AjaxLink<Void>("showNext")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            /**
             * Get the current beginning sentence address and add on it the size of the display
             * window
             */
            @Override
            public void onClick(AjaxRequestTarget target)
            {
                if (bratAnnotatorModel.getDocument() != null) {
                    int nextSentenceAddress = BratAjaxCasUtil
                            .getNextDisplayWindowSentenceBeginAddress(
                                    getJCas(bratAnnotatorModel.getProject(),
                                            bratAnnotatorModel.getDocument()),
                                    bratAnnotatorModel.getSentenceAddress(),
                                    bratAnnotatorModel.getWindowSize());
                    if (bratAnnotatorModel.getSentenceAddress() != nextSentenceAddress) {
                        bratAnnotatorModel.setSentenceAddress(nextSentenceAddress);
                        // target.add(annotator);
                        target.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");
                    }

                    else {
                        target.appendJavaScript("alert('This is last page!')");
                    }
                }
                else {
                    target.appendJavaScript("alert('Please open a document first!')");
                }
            }
        }.add(new InputBehavior(new KeyType[] { KeyType.Page_down }, EventType.click)));

        // Show the previous page of this document
        add(new AjaxLink<Void>("showPrevious")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                if (bratAnnotatorModel.getDocument() != null) {
                    int previousSentenceAddress = BratAjaxCasUtil
                            .getPreviousDisplayWindowSentenceBeginAddress(
                                    getJCas(bratAnnotatorModel.getProject(),
                                            bratAnnotatorModel.getDocument()),
                                    bratAnnotatorModel.getSentenceAddress(),
                                    bratAnnotatorModel.getWindowSize());
                    if (bratAnnotatorModel.getSentenceAddress() != previousSentenceAddress) {
                        bratAnnotatorModel.setSentenceAddress(previousSentenceAddress);
                        // target.add(annotator);
                        target.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");
                    }
                    else {
                        target.appendJavaScript("alert('This is First Page!')");
                    }
                }
                else {
                    target.appendJavaScript("alert('Please open a document first!')");
                }
            }
        }.add(new InputBehavior(new KeyType[] { KeyType.Page_up }, EventType.click)));

        add(new AjaxLink<Void>("showFirst")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                if (bratAnnotatorModel.getDocument() != null) {
                    if (bratAnnotatorModel.getFirstSentenceAddress() != bratAnnotatorModel
                            .getSentenceAddress()) {
                        bratAnnotatorModel.setSentenceAddress(bratAnnotatorModel
                                .getFirstSentenceAddress());
                        // target.add(annotator);
                        target.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");
                    }
                    else {
                        target.appendJavaScript("alert('This is first page!')");
                    }
                }
                else {
                    target.appendJavaScript("alert('Please open a document first!')");
                }
            }
        }.add(new InputBehavior(new KeyType[] { KeyType.Home }, EventType.click)));

        add(new AjaxLink<Void>("showLast")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                if (bratAnnotatorModel.getDocument() != null) {
                    int lastDisplayWindowBeginingSentenceAddress = BratAjaxCasUtil
                            .getLastDisplayWindowFirstSentenceAddress(
                                    getJCas(bratAnnotatorModel.getProject(),
                                            bratAnnotatorModel.getDocument()),
                                    bratAnnotatorModel.getWindowSize());
                    if (lastDisplayWindowBeginingSentenceAddress != bratAnnotatorModel
                            .getSentenceAddress()) {
                        bratAnnotatorModel
                                .setSentenceAddress(lastDisplayWindowBeginingSentenceAddress);
                        // target.add(annotator);
                        target.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");
                    }
                    else {
                        target.appendJavaScript("alert('This is last Page!')");
                    }
                }
                else {
                    target.appendJavaScript("alert('Please open a document first!')");
                }
            }
        }.add(new InputBehavior(new KeyType[] { KeyType.End }, EventType.click)));

        add(new GuidelineModalPanel("guidelineModalPanel", new Model<BratAnnotatorModel>(
                bratAnnotatorModel)));

        gotoPageTextField = (NumberTextField<Integer>) new NumberTextField<Integer>("gotoPageText",
                new Model<Integer>(10));
        gotoPageTextField.setType(Integer.class);
        add(gotoPageTextField);
        gotoPageTextField.add(new AjaxFormComponentUpdatingBehavior("onchange")
        {

            @Override
            protected void onUpdate(AjaxRequestTarget target)
            {
                gotoPageAddress = BratAjaxCasUtil.getSentenceAddress(
                        getJCas(bratAnnotatorModel.getProject(), bratAnnotatorModel.getDocument()),
                        gotoPageTextField.getModelObject());

            }
        });

        add(new AjaxLink<Void>("gotoPageLink")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                if (gotoPageAddress == -2) {
                    target.appendJavaScript("alert('This sentence number is either negative or beyond the last sentence number!')");
                }
                else if (bratAnnotatorModel.getDocument() != null) {

                    if (gotoPageAddress == -1) {
                        // Not Updated, default used
                        gotoPageAddress = BratAjaxCasUtil.getSentenceAddress(
                                getJCas(bratAnnotatorModel.getProject(),
                                        bratAnnotatorModel.getDocument()), 10);
                    }
                    if (bratAnnotatorModel.getSentenceAddress() != gotoPageAddress) {
                        bratAnnotatorModel.setSentenceAddress(gotoPageAddress);
                        // target.add(annotator);
                        target.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");
                    }
                    else {
                        target.appendJavaScript("alert('This sentence is on the same page!')");
                    }
                }
                else {
                    target.appendJavaScript("alert('Please open a document first!')");
                }
            }
        });

        finish = new FinishImage("finishImage", new Model<BratAnnotatorModel>(bratAnnotatorModel));

        add(new FinishLink("showYesNoModalPanel",
                new Model<BratAnnotatorModel>(bratAnnotatorModel), finish)
        {
            private static final long serialVersionUID = -4657965743173979437L;
        });
    }

    @Override
    public void renderHead(IHeaderResponse response)
    {
        String jQueryString = "";
        if (firstLoad) {
            jQueryString += "jQuery('#showOpenDocumentModal').trigger('click');";
            firstLoad = false;
        }
        response.renderOnLoadJavaScript(jQueryString);
    }

    private JCas getJCas(Project aProject, SourceDocument aDocument)
    {
        JCas jCas = null;
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();

            User user = repository.getUser(username);
            BratAjaxCasController controller = new BratAjaxCasController(repository,
                    annotationService);
            jCas = controller.getJCas(aDocument, aProject, user);
        }
        catch (UIMAException e) {
            error("CAS object not found :" + ExceptionUtils.getRootCauseMessage(e));
        }
        catch (IOException e) {
            error("CAS object not found :" + ExceptionUtils.getRootCauseMessage(e));
        }
        catch (ClassNotFoundException e) {
            error("The Class name in the properties is not found " + ":"
                    + ExceptionUtils.getRootCauseMessage(e));
        }
        return jCas;

    }

    public int getWindowSize()
    {
        return windowSize;
    }

    public void setWindowSize(int aWindowSize)
    {
        windowSize = aWindowSize;
    }

    public void setAttributesForDocument()
        throws UIMAException, IOException, ClassNotFoundException
    {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        bratAnnotatorModel.setUser(repository.getUser(username));
        JCas jacas = getCas(bratAnnotatorModel.getProject(), bratAnnotatorModel.getUser(),
                bratAnnotatorModel.getDocument());
        if (bratAnnotatorModel.getSentenceAddress() == -1
                || bratAnnotatorModel.getDocument().getId() != currentDocumentId
                || bratAnnotatorModel.getProject().getId() != currentprojectId) {

            bratAnnotatorModel.setSentenceAddress(BratAjaxCasUtil.getFirstSenetnceAddress(jacas));
            bratAnnotatorModel
                    .setLastSentenceAddress(BratAjaxCasUtil.getLastSenetnceAddress(jacas));
            bratAnnotatorModel.setFirstSentenceAddress(bratAnnotatorModel.getSentenceAddress());
            bratAnnotatorModel.setWindowSize(10);

            AnnotationPreference preference = new AnnotationPreference();
            ApplicationUtils.setAnnotationPreference(preference, username, repository,
                    annotationService, bratAnnotatorModel, Mode.ANNOTATION);
        }

        currentprojectId = bratAnnotatorModel.getProject().getId();
        currentDocumentId = bratAnnotatorModel.getDocument().getId();
    }

    /*    *//**
     * Set different attributes for
     * {@link BratAjaxCasController#getCollectionInformation(String, ArrayList) }
     *
     * @throws IOException
     */
    /*
     *
     * public void setAttributesForGetCollection() throws IOException { String username =
     * SecurityContextHolder.getContext().getAuthentication().getName();
     *
     * if (bratAnnotatorModel.getProject().getId() != currentprojectId) { AnnotationPreference
     * preference = new AnnotationPreference(); ApplicationUtils.setAnnotationPreference(preference,
     * username, repository, annotationService, bratAnnotatorModel, Mode.ANNOTATION); }
     * currentprojectId = bratAnnotatorModel.getProject().getId();
     *
     * }
     */

    boolean isDocumentOpenedFirstTime(String aCollection, String adocumentName)
    {
        bratAnnotatorModel.setProject(repository.getProject(aCollection.replace("/", "")));
        bratAnnotatorModel.setDocument(repository.getSourceDocument(adocumentName,
                bratAnnotatorModel.getProject()));

        try {
            repository.getAnnotationDocument(bratAnnotatorModel.getDocument(),
                    bratAnnotatorModel.getUser());
            return false;
        }
        catch (NoResultException e) {
            return true;
        }
    }

    private JCas getCas(Project aProject, User user, SourceDocument aDocument)
        throws UIMAException, IOException, ClassNotFoundException
    {
        JCas jCas = null;
        BratAjaxCasController controller = new BratAjaxCasController(repository, annotationService);
        jCas = controller.getJCas(aDocument, aProject, user);
        return jCas;
    }

}