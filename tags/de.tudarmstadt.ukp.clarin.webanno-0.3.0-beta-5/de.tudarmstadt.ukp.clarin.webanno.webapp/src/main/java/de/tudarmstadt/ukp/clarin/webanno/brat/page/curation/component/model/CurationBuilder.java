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
package de.tudarmstadt.ukp.clarin.webanno.brat.page.curation.component.model;

import static org.uimafit.util.JCasUtil.select;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.jcas.JCas;
import org.uimafit.util.CasUtil;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.page.curation.AnnotationOption;
import de.tudarmstadt.ukp.clarin.webanno.brat.page.curation.AnnotationSelection;
import de.tudarmstadt.ukp.clarin.webanno.brat.page.curation.CasDiff;
import de.tudarmstadt.ukp.clarin.webanno.brat.page.curation.component.CurationPanel;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceLink;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class CurationBuilder {

	private RepositoryService repository;

	private final static Log LOG = LogFactory.getLog(CurationPanel.class);
	
	public CurationBuilder(RepositoryService repository) {
		this.repository = repository;
	}

	public CurationContainer buildCurationContainer(SourceDocument sourceDocument) {
		CurationContainer curationContainer = new CurationContainer();
			// initialize Variables
			Map<Integer, Integer> segmentBeginEnd = new HashMap<Integer, Integer>();
			Map<Integer, Integer> segmentNumber = new HashMap<Integer, Integer>();
			Map<Integer, String> segmentText = new HashMap<Integer, String>();
			Map<Integer, Integer> segmentAdress = new HashMap<Integer, Integer>();
			// get annotation documents
			List<AnnotationDocument> annotationDocuments = repository
					.listAnnotationDocument(sourceDocument);
			for (AnnotationDocument annotationDocument : annotationDocuments) {
				JCas jcas;
				try {
					jcas = repository
							.getAnnotationDocumentContent(annotationDocument);
					// enumerate Sentences
					int sentenceNumber = 0;
					for (Sentence sentence : select(jcas, Sentence.class)) {
						sentenceNumber += 1;
						segmentBeginEnd.put(sentence.getBegin(), sentence.getEnd());
						segmentText.put(sentence.getBegin(), sentence.getCoveredText().toString());
						segmentNumber.put(sentence.getBegin(), sentenceNumber);
						segmentAdress.put(sentence.getBegin(), sentence.getAddress());
						// store tokens
					}
				} catch (Exception e) {
					LOG.info("Skipping document due to exception ["+annotationDocument+"]", e);
				}

			}
			
			JCas firstJCas = null;
			Map<String, JCas> jCases = new HashMap<String, JCas>();
			for (AnnotationDocument annotationDocument : annotationDocuments) {
				try {
					JCas jCas = repository.getAnnotationDocumentContent(annotationDocument);
					String username = annotationDocument.getUser().getUsername();
					
					if(firstJCas == null) {
						firstJCas = jCas;
					}
					jCases.put(username, jCas);
				} catch (Exception e) {
					LOG.info("Skipping document due to exception ["+annotationDocument+"]", e);
				}
			}
			List<Type> entryTypes = new LinkedList<Type>();
	    	//entryTypes.add(CasUtil.getType(firstJCas.getCas(), Token.class));
			//entryTypes.add(CasUtil.getType(firstJCas.getCas(), Sentence.class));
			entryTypes.add(CasUtil.getType(firstJCas.getCas(), POS.class));
			entryTypes.add(CasUtil.getType(firstJCas.getCas(), CoreferenceLink.class));
			entryTypes.add(CasUtil.getType(firstJCas.getCas(), Lemma.class));
			entryTypes.add(CasUtil.getType(firstJCas.getCas(), NamedEntity.class));
			// TODO arc types
			
	    	int numUsers = jCases.size();
	    	
			for (Integer begin : segmentBeginEnd.keySet()) {
				Integer end = segmentBeginEnd.get(begin);
				
		    	List<AnnotationOption> annotationOptions = null;
				try {
					annotationOptions = CasDiff.doDiff(entryTypes, jCases, begin, end);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				Boolean hasDiff = false;
				for (AnnotationOption annotationOption : annotationOptions) {
					List<AnnotationSelection> annotationSelections = annotationOption.getAnnotationSelections();
					if (annotationSelections.size() > 1) {
						hasDiff = true;
					} else if(annotationSelections.size() == 1) {
						AnnotationSelection annotationSelection = annotationSelections.get(0);
						if(annotationSelection.getAddressByUsername().size() < numUsers) {
							hasDiff = true;
						}
					}
				}
				
				CurationSegment curationSegment = new CurationSegment();
				curationSegment.setBegin(begin);
				curationSegment.setEnd(end);
				if(hasDiff) {
					curationSegment.setSentenceState(SentenceState.DISAGREE);
				} else {
					curationSegment.setSentenceState(SentenceState.AGREE);
				}
				String text = "(" + segmentNumber.get(begin).toString() + ") " + segmentText.get(begin);
				curationSegment.setText(text);
				curationSegment.setSentenceAddress(segmentAdress.get(begin));
				curationContainer.getCurationSegmentByBegin().put(begin, curationSegment);
			}

		return curationContainer;
	}

}