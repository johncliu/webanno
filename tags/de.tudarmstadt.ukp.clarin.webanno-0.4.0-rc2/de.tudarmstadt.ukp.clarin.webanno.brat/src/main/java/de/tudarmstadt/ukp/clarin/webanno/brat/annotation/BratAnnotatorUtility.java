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
package de.tudarmstadt.ukp.clarin.webanno.brat.annotation;

import java.io.IOException;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.wicket.request.IRequestParameters;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotator.MultipleSentenceCoveredException;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.AnnotationTypeConstant;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasController;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.OffsetsList;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.User;

/**
 * A helper class for {@link BratAnnotator} and CurationEditor
 *
 * @author Seid Muhie Yimam
 *
 */
public class BratAnnotatorUtility
{

    public static Object getDocument(BratAnnotatorUIData aUIData, RepositoryService repository,
            AnnotationService annotationService, BratAnnotatorModel bratAnnotatorModel)
        throws ClassNotFoundException, IOException, UIMAException
    {
        Object result = null;
        BratAjaxCasController controller = new BratAjaxCasController(repository, annotationService);

        aUIData.setGetDocument(true);
        result = controller.getDocumentResponse(bratAnnotatorModel,
                aUIData.getAnnotationOffsetStart(), aUIData.getjCas(), aUIData.isGetDocument());
        aUIData.setGetDocument(false);

        return result;
    }

    public static Object createSpan(IRequestParameters aRequest, User aUser,
            BratAnnotatorUIData aUIData, RepositoryService repository,
            AnnotationService annotationService, BratAnnotatorModel bratAnnotatorModel,
            MappingJacksonHttpMessageConverter jsonConverter)
        throws ClassNotFoundException, IOException, UIMAException, MultipleSentenceCoveredException
    {

        Object result = null;
        BratAjaxCasController controller = new BratAjaxCasController(repository, annotationService);
        String offsets = aRequest.getParameterValue("offsets").toString();

        OffsetsList offsetLists = jsonConverter.getObjectMapper().readValue(offsets,
                OffsetsList.class);
        int start = offsetLists.get(0).getBegin();
        int end = offsetLists.get(0).getEnd();
        aUIData.setAnnotationOffsetStart(BratAjaxCasUtil.getAnnotationBeginOffset(
                aUIData.getjCas(), bratAnnotatorModel.getSentenceAddress()) + start);
        aUIData.setAnnotationOffsetEnd(BratAjaxCasUtil.getAnnotationBeginOffset(aUIData.getjCas(),
                bratAnnotatorModel.getSentenceAddress()) + end);
        aUIData.setType(aRequest.getParameterValue("type").toString());

        if (!BratAjaxCasUtil.offsetsInOneSentences(aUIData.getjCas(),
                aUIData.getAnnotationOffsetStart(), aUIData.getAnnotationOffsetEnd())) {
            throw new MultipleSentenceCoveredException(
                    "Annotation coveres multiple sentences, limit your annotation to single sentence!");
        }

        AnnotationFS originFs = (AnnotationFS) aUIData.getjCas().getLowLevelCas().ll_getFSForRef(
                aUIData.getOrigin());
        AnnotationFS targetFs = (AnnotationFS) aUIData.getjCas().getLowLevelCas().ll_getFSForRef(
                aUIData.getTarget());

        result = controller.createSpanResponse(bratAnnotatorModel,
                aUIData.getAnnotationOffsetStart(), aUIData.getjCas(), aUIData.isGetDocument(),
                aUIData.getAnnotationOffsetEnd(), aUIData.getType(),originFs,targetFs);

        if (bratAnnotatorModel.isScrollPage()) {
            bratAnnotatorModel.setSentenceAddress(BratAjaxCasUtil.getSentenceBeginAddress(
                    aUIData.getjCas(), bratAnnotatorModel.getSentenceAddress(),
                    aUIData.getAnnotationOffsetStart(), bratAnnotatorModel.getProject(),
                    bratAnnotatorModel.getDocument(), bratAnnotatorModel.getWindowSize()));
        }
        return result;
    }

    public static Object createArc(IRequestParameters aRequest, User aUser,
            BratAnnotatorUIData aUIData, RepositoryService repository,
            AnnotationService annotationService, BratAnnotatorModel bratAnnotatorModel)
        throws UIMAException, IOException
    {

        Object result = null;
        BratAjaxCasController controller = new BratAjaxCasController(repository, annotationService);
        aUIData.setOrigin(aRequest.getParameterValue("origin").toInt());
        aUIData.setTarget(aRequest.getParameterValue("target").toInt());
        aUIData.setType(aRequest.getParameterValue("type").toString());
        aUIData.setAnnotationOffsetStart(BratAjaxCasUtil.getAnnotationBeginOffset(
                aUIData.getjCas(), aUIData.getOrigin()));

        AnnotationFS originFs = (AnnotationFS) aUIData.getjCas().getLowLevelCas().ll_getFSForRef(
                aUIData.getOrigin());
        AnnotationFS targetFs = (AnnotationFS) aUIData.getjCas().getLowLevelCas().ll_getFSForRef(
                aUIData.getTarget());

        result = controller.createArcResponse(bratAnnotatorModel,
                aUIData.getAnnotationOffsetStart(), aUIData.getjCas(), aUIData.isGetDocument(),
                aUIData.getType(), aUIData.getAnnotationOffsetEnd(), originFs,targetFs);
        if (bratAnnotatorModel.isScrollPage()) {
            bratAnnotatorModel.setSentenceAddress(BratAjaxCasUtil.getSentenceBeginAddress(
                    aUIData.getjCas(), bratAnnotatorModel.getSentenceAddress(),
                    aUIData.getAnnotationOffsetStart(), bratAnnotatorModel.getProject(),
                    bratAnnotatorModel.getDocument(), bratAnnotatorModel.getWindowSize()));
        }
        return result;
    }

    public static Object reverseArc(IRequestParameters aRequest, User aUser,
            BratAnnotatorUIData aUIData, RepositoryService repository,
            AnnotationService annotationService, BratAnnotatorModel bratAnnotatorModel)
        throws UIMAException, IOException
    {

        Object result = null;
        BratAjaxCasController controller = new BratAjaxCasController(repository, annotationService);
        aUIData.setOrigin(aRequest.getParameterValue("origin").toInt());
        aUIData.setTarget(aRequest.getParameterValue("target").toInt());
        aUIData.setType(aRequest.getParameterValue("type").toString());
        aUIData.setAnnotationOffsetStart(BratAjaxCasUtil.getAnnotationBeginOffset(
                aUIData.getjCas(), aUIData.getOrigin()));

        String annotationType = aUIData.getType().substring(0,
                aUIData.getType().indexOf(AnnotationTypeConstant.PREFIX) + 1);
        if (annotationType.equals(AnnotationTypeConstant.POS_PREFIX)) {

            AnnotationFS originFs = (AnnotationFS) aUIData.getjCas().getLowLevelCas().ll_getFSForRef(
                    aUIData.getOrigin());
            AnnotationFS targetFs = (AnnotationFS) aUIData.getjCas().getLowLevelCas().ll_getFSForRef(
                    aUIData.getTarget());

            result = controller.reverseArcResponse(bratAnnotatorModel, aUIData.getjCas(),
                    aUIData.getAnnotationOffsetStart(), originFs, targetFs,
                    aUIData.getType(), aUIData.isGetDocument());
            if (bratAnnotatorModel.isScrollPage()) {
                bratAnnotatorModel.setSentenceAddress(BratAjaxCasUtil.getSentenceBeginAddress(
                        aUIData.getjCas(), bratAnnotatorModel.getSentenceAddress(),
                        aUIData.getAnnotationOffsetStart(), bratAnnotatorModel.getProject(),
                        bratAnnotatorModel.getDocument(), bratAnnotatorModel.getWindowSize()));
            }
        }
        return result;
    }

    public static Object deleteSpan(IRequestParameters aRequest, User aUser,
            BratAnnotatorUIData aUIData, RepositoryService repository,
            AnnotationService annotationService, BratAnnotatorModel bratAnnotatorModel,
            MappingJacksonHttpMessageConverter jsonConverter)
        throws JsonParseException, JsonMappingException, UIMAException, IOException
    {

        Object result = null;
        BratAjaxCasController controller = new BratAjaxCasController(repository, annotationService);

        String offsets = aRequest.getParameterValue("offsets").toString();
        int id = aRequest.getParameterValue("id").toInt();
        OffsetsList offsetLists = jsonConverter.getObjectMapper().readValue(offsets,
                OffsetsList.class);
        int start = offsetLists.get(0).getBegin();
        int end = offsetLists.get(0).getEnd();
        aUIData.setAnnotationOffsetStart(BratAjaxCasUtil.getAnnotationBeginOffset(
                aUIData.getjCas(), bratAnnotatorModel.getSentenceAddress()) + start);
        aUIData.setAnnotationOffsetEnd(BratAjaxCasUtil.getAnnotationBeginOffset(aUIData.getjCas(),
                bratAnnotatorModel.getSentenceAddress()) + end);
        aUIData.setType(aRequest.getParameterValue("type").toString());

        AnnotationFS idFs = (AnnotationFS) aUIData.getjCas().getLowLevelCas().ll_getFSForRef(id);

        result = controller.deleteSpanResponse(bratAnnotatorModel, idFs,
                aUIData.getAnnotationOffsetStart(), aUIData.getjCas(), aUIData.isGetDocument(),
                aUIData.getType());
        if (bratAnnotatorModel.isScrollPage()) {
            bratAnnotatorModel.setSentenceAddress(BratAjaxCasUtil.getSentenceBeginAddress(
                    aUIData.getjCas(), bratAnnotatorModel.getSentenceAddress(),
                    aUIData.getAnnotationOffsetStart(), bratAnnotatorModel.getProject(),
                    bratAnnotatorModel.getDocument(), bratAnnotatorModel.getWindowSize()));
        }
        return result;
    }

    public static Object deleteArc(IRequestParameters aRequest, User aUser,
            BratAnnotatorUIData aUIData, RepositoryService repository,
            AnnotationService annotationService, BratAnnotatorModel bratAnnotatorModel)
        throws UIMAException, IOException
    {

        Object result = null;
        BratAjaxCasController controller = new BratAjaxCasController(repository, annotationService);

        aUIData.setOrigin(aRequest.getParameterValue("origin").toInt());
        aUIData.setTarget(aRequest.getParameterValue("target").toInt());
        aUIData.setType(aRequest.getParameterValue("type").toString());
        aUIData.setAnnotationOffsetStart(BratAjaxCasUtil.getAnnotationBeginOffset(
                aUIData.getjCas(), aUIData.getOrigin()));

        AnnotationFS originFs = (AnnotationFS) aUIData.getjCas().getLowLevelCas().ll_getFSForRef(aUIData.getOrigin());
        AnnotationFS targetFs = (AnnotationFS) aUIData.getjCas().getLowLevelCas().ll_getFSForRef(aUIData.getTarget());

        result = controller.deleteArcResponse(bratAnnotatorModel, aUIData.getjCas(),
                aUIData.getAnnotationOffsetStart(), originFs, targetFs,
                aUIData.getType(), aUIData.isGetDocument());
        if (bratAnnotatorModel.isScrollPage()) {
            bratAnnotatorModel.setSentenceAddress(BratAjaxCasUtil.getSentenceBeginAddress(
                    aUIData.getjCas(), bratAnnotatorModel.getSentenceAddress(),
                    aUIData.getAnnotationOffsetStart(), bratAnnotatorModel.getProject(),
                    bratAnnotatorModel.getDocument(), bratAnnotatorModel.getWindowSize()));
        }
        return result;
    }

    public static boolean isDocumentFinished(RepositoryService aRepository,
            BratAnnotatorModel aBratAnnotatorModel)
    {
        // if annotationDocument is finished, disable editing
        boolean finished = false;
        try {
            if(aBratAnnotatorModel.getMode().equals(Mode.CURATION)){
                if (aBratAnnotatorModel.getDocument().getState()
                        .equals(SourceDocumentState.CURATION_FINISHED)) {
                    finished = true;
                }
            }
            else if (aRepository
                    .getAnnotationDocument(aBratAnnotatorModel.getDocument(),
                            aBratAnnotatorModel.getUser()).getState()
                    .equals(AnnotationDocumentState.FINISHED)
                    || aBratAnnotatorModel.getDocument().getState()
                            .equals(SourceDocumentState.CURATION_FINISHED)
                    || aBratAnnotatorModel.getDocument().getState()
                            .equals(SourceDocumentState.CURATION_IN_PROGRESS)) {
                finished = true;
            }
        }
        catch (Exception e) {
            finished = false;
        }

        return finished;
    }
}