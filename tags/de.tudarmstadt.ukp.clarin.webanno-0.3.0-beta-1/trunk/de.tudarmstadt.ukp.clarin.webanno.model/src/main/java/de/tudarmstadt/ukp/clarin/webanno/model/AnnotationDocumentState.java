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
package de.tudarmstadt.ukp.clarin.webanno.model;

/**
 * Variables for the different states of a {@link AnnotationDocument} workflow.
 *
 * @author Seid Muhie Yimam
 *
 */
public enum AnnotationDocumentState
{
    /**
     *
     * annotation document has been created for this document for this annotator
     */
    INPROGRESS,
    /**
     * annotator has marked annotation document as complete
     */
    FINISHED;
}