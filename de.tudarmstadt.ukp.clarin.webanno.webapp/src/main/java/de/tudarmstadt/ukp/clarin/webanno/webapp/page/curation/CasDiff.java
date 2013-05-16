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
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation;

import static org.uimafit.util.CasUtil.selectCovered;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.CasCopier;
import org.uimafit.util.CasUtil;

public class CasDiff {

	/**
	 * spot differing annotations by comparing cases of the same source document.
	 * <br /><br />
	 * 
	 * 
	 * @param aCasMap
	 *            Map of (username, cas)
	 * @return List of {@link AnnotationOption}
	 * @throws Exception
	 */

	public static List<AnnotationOption> doDiff(List<Type> aEntryTypes,
			Map<String, JCas> aCasMap, int aBegin, int aEnd) throws Exception {
		Map<Integer, Map<Integer, Set<AnnotationFS>>> annotationFSsByBeginEnd = new HashMap<Integer, Map<Integer, Set<AnnotationFS>>>();
		List<AnnotationOption> annotationOptions = new LinkedList<AnnotationOption>();
		Map<FeatureStructure, String> usernameByFeatureStructure = new HashMap<FeatureStructure, String>();

		Set<String> usernames = new HashSet<String>();

		for (Type aEntryType : aEntryTypes) {
			for (String username : aCasMap.keySet()) {
				usernames.add(username);
				CAS cas = aCasMap.get(username).getCas();
				
				// instead cas.getAnnotationIndex(aEntryType) also
				// cas.getIndexRepository().getAllIndexedFS(aType)
				for (AnnotationFS annotationFS : selectCovered(cas, aEntryType,
						aBegin, aEnd)) {
					Integer begin = annotationFS.getBegin();
					Integer end = annotationFS.getEnd();
					
					if (!annotationFSsByBeginEnd.containsKey(begin)) {
						annotationFSsByBeginEnd.put(begin,
								new HashMap<Integer, Set<AnnotationFS>>());
					}
					if (!annotationFSsByBeginEnd.get(begin).containsKey(end)) {
						annotationFSsByBeginEnd.get(begin).put(end,
								new HashSet<AnnotationFS>());
					}
					annotationFSsByBeginEnd.get(begin).get(end).add(annotationFS);
					usernameByFeatureStructure.put(annotationFS, username);
				}
			}
			
			Map<FeatureStructure, AnnotationSelection> annotationSelectionByFeatureStructure = new HashMap<FeatureStructure, AnnotationSelection>();
			for (Map<Integer, Set<AnnotationFS>> annotationFSsByEnd : annotationFSsByBeginEnd
					.values()) {
				for (Set<AnnotationFS> annotationFSs : annotationFSsByEnd.values()) {
					Map<String, AnnotationOption> annotationOptionPerType = new HashMap<String, AnnotationOption>();
					for (FeatureStructure fsNew : annotationFSs) {
						String usernameFSNew = usernameByFeatureStructure
								.get(fsNew);
						// diffFS1 contains all feature structures of fs1, which do not occur in other cases
						Set<FeatureStructure> diffFSNew = traverseFS(fsNew);
						Map<FeatureStructure, AnnotationOption> annotationOptionByCompareResultFS1 = new HashMap<FeatureStructure, AnnotationOption>();
						
						Map<FeatureStructure, AnnotationSelection> annotationSelectionByFeatureStructureNew = new HashMap<FeatureStructure, AnnotationSelection>(annotationSelectionByFeatureStructure);
						for (FeatureStructure fsOld : annotationSelectionByFeatureStructure.keySet()) {
							String usernameFSOld = usernameByFeatureStructure
									.get(fsOld);
							if (fsNew != fsOld) {
								CompareResult compareResult = compareFeatureFS(
										fsNew, fsOld, diffFSNew);
								for (FeatureStructure compareResultFSNew : compareResult.getAgreements().keySet()) {
									FeatureStructure compareResultFSOld = compareResult.getAgreements().get(compareResultFSNew);
									int addressNew = aCasMap.get(usernameFSNew).getLowLevelCas().ll_getFSRef(compareResultFSNew);
									AnnotationSelection annotationSelection = annotationSelectionByFeatureStructure.get(compareResultFSOld);
									annotationSelection.getAddressByUsername().put(usernameFSNew, addressNew);
									annotationSelectionByFeatureStructureNew.put(compareResultFSNew, annotationSelection);
									
								}
							}
						}
						annotationSelectionByFeatureStructure = annotationSelectionByFeatureStructureNew;
						
						// add featureStructures, that have not been found in existing annotationSelections
						for (FeatureStructure subFS1 : diffFSNew) {
							AnnotationSelection annotationSelection = new AnnotationSelection();
							int addressSubFS1 = aCasMap.get(usernameFSNew).getLowLevelCas().ll_getFSRef(subFS1);
							annotationSelection.getAddressByUsername().put(usernameFSNew, addressSubFS1);
							annotationSelectionByFeatureStructure.put(subFS1, annotationSelection);
							String type = subFS1.getType().toString();
							if(!annotationOptionPerType.containsKey(type)) {
								annotationOptionPerType.put(type, new AnnotationOption());
							}
							AnnotationOption annotationOption = annotationOptionPerType.get(type);
							// link annotationOption and annotationSelection
							annotationSelection.setAnnotationOption(annotationOption);
							annotationOption.getAnnotationSelections().add(annotationSelection);
						}
					}
					annotationOptions.addAll(annotationOptionPerType.values());
				}
			}
		}

		return annotationOptions;
	}

	private static Set<FeatureStructure> traverseFS(FeatureStructure fs) {
		Set<FeatureStructure> nodePlusChildren = new HashSet<FeatureStructure>();
		nodePlusChildren.add(fs);
		for (Feature feature : fs.getType().getFeatures()) {
			// features are present in both feature structures, fs1 and fs2
			// compare primitive values
			if (!feature.getRange().isPrimitive()) {
				// compare composite types
				// assumtion: if feature is not primitive, it is a composite feature
				FeatureStructure featureValue = fs.getFeatureValue(feature);
				if (featureValue != null) {
					nodePlusChildren.addAll(traverseFS(featureValue));
				}
			}
		}
		return nodePlusChildren;
	}

	private static CompareResult compareFeatureFS(
			FeatureStructure fsNew, FeatureStructure fsOld, Set<FeatureStructure> diffFSNew) throws Exception {
		CompareResult compareResult = new CompareResult();
		
		// check if types are equal
		Type type = fsNew.getType();
		if (!fsOld.getType().toString().equals(type.toString())) {
			// if types differ add feature structure to diff
			compareResult.getDiffs().put(fsNew, fsOld);
			return compareResult;
		}

		boolean agreeOnSubfeatures = true;
		for (Feature feature : type.getFeatures()) {
			// features are present in both feature structures, fs1 and fs2
			// compare primitive values
			if (feature.getRange().isPrimitive()) {

				// check int Values
				if (feature.getRange().getName().equals("uima.cas.Integer")) {
					if (!(fsNew.getIntValue(feature) == fsOld.getIntValue(feature))) {
						//disagree
						agreeOnSubfeatures = false;
					} else {
						// agree
					}
				} else if (feature.getRange().getName()
						.equals("uima.cas.String")) {
					String stringValue1 = fsNew.getStringValue(feature);
					String stringValue2 = fsNew.getStringValue(feature);
					if (stringValue1 == null && stringValue2 == null) {
						// agree
						// Do nothing, null == null
					} else if (stringValue1 == null
							|| stringValue2 == null
							|| !fsNew.getStringValue(feature).equals(
									fsOld.getStringValue(feature))) {
						// stringValue1 differs from stringValue2
						
						// disagree
						agreeOnSubfeatures = false;
						//compareResult.getDiffs().put(fs1, fs2);
					} else {
						// agree
						//compareResult.getAgreements().put(fs1, fs2);
						//diffFS1.remove(fs1);
					}
				} else {
					throw new Exception(feature.getRange().getName()
							+ " not yet checkd!");
				}

				// TODO check other Values
			} else {
				// compare composite types
				// TODO assumtion: if feature is not primitive, it is a
				// composite feature
				FeatureStructure featureValue1 = fsNew.getFeatureValue(feature);
				FeatureStructure featureValue2 = fsOld.getFeatureValue(feature);
				if (featureValue1 != null && featureValue2 != null) {
					CompareResult compareResultSubfeatures = compareFeatureFS(
							featureValue1, featureValue2, diffFSNew);
					compareResult.getDiffs().putAll(compareResultSubfeatures.getDiffs());
					compareResult.getAgreements().putAll(compareResultSubfeatures.getAgreements());
					if(!compareResult.getDiffs().isEmpty()) {
						agreeOnSubfeatures = false;
					}
				}
				/* not necessary anymore
				else if (featureValue1 == null && featureValue2 != null) {
					// TODO if feature present in only one branch add to separate field in CompareResult
					Set<FeatureStructure> allFV2 = traverseFS(featureValue2);
					compareResult.getFs2only().addAll(allFV2);
//					compareResult.getDiffs().get(fs2).add(featureValue2);
				} else if (featureValue2 == null && featureValue1 != null) {
					// TODO if feature present in only one branch add to separate field in CompareResult
					Set<FeatureStructure> allFV1 = traverseFS(featureValue1);
					compareResult.getFs2only().addAll(allFV1);
//					compareResult.getDiffs().get(fs1).add(featureValue1);
				}
				*/
			}
		}
		if(agreeOnSubfeatures) {
			compareResult.getAgreements().put(fsNew, fsOld);
			diffFSNew.remove(fsNew);
		} else {
			compareResult.getDiffs().put(fsNew, fsOld);
		}
		
		// TODO if no diffs, agree (here or elsewhere)?
		if(compareResult.getDiffs().isEmpty()) {
			compareResult.getAgreements().put(fsNew, fsOld);
			diffFSNew.remove(fsNew);
		}

		return compareResult;
	}
	
}
