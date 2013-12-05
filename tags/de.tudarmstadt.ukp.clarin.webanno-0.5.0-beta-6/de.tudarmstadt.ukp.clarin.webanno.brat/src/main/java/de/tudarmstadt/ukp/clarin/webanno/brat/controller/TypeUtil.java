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
package de.tudarmstadt.ukp.clarin.webanno.brat.controller;

import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.apache.uima.cas.Type;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationType;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceChain;
import de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceLink;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

public final class TypeUtil
{
    private TypeUtil()
    {
        // No instances
    }

    public static final String POS = "pos";
    public static final String NAMEDENTITY = "named entity";
    public static final String DEPENDENCY = "dependency";
    public static final String COREFERENCE = "coreference";
    public static final String COREFRELTYPE = "coreference type";
    public static final String LEMMA = "lemma";

    public static TypeAdapter getAdapter(Type aType)
    {
        /*
         * if (aType.getName().equals(POS.class.getName())) { return SpanAdapter.getPosAdapter(); }
         * else
         */if (aType.getName().equals(NamedEntity.class.getName())) {
            return SpanAdapter.getNamedEntityAdapter();
        }
        else if (aType.getName().equals(Dependency.class.getName())) {
            return ArcAdapter.getDependencyAdapter();
        }
        else if (aType.getName().equals(CoreferenceChain.class.getName())) {
            return ChainAdapter.getCoreferenceChainAdapter();
        }
        else if (aType.getName().equals(CoreferenceLink.class.getName())) {
            return ChainAdapter.getCoreferenceLinkAdapter();
        }
        else {
            throw new IllegalArgumentException("No adapter for type [" + aType.getName() + "]");
        }
    }

    /**
     * Get the adapter based on the {@link PrefixFileFilter} such as POS_, NAMED_ENTITY_
     *
     * @param aPrefix
     * @return
     */
    public static TypeAdapter getAdapter(String aPrefix)
    {
        if (aPrefix.equals(POS)) {
            return SpanAdapter.getPosAdapter();
        }
        else if (aPrefix.equals(LEMMA)) {
            return SpanAdapter.getLemmaAdapter();
        }
        else if (aPrefix.equals(NAMEDENTITY)) {
            return SpanAdapter.getNamedEntityAdapter();
        }
        else if (aPrefix.equals(DEPENDENCY)) {
            return ArcAdapter.getDependencyAdapter();
        }
        else if (aPrefix.equals(COREFERENCE)) {
            return ChainAdapter.getCoreferenceChainAdapter();
        }
        else if (aPrefix.equals(COREFRELTYPE)) {
            return ChainAdapter.getCoreferenceLinkAdapter();
        }
        else {
            throw new IllegalArgumentException("No adapter for type [" + aPrefix + "]");
        }
    }

    /**
     * Get the annotation type, using the request sent from brat. If the request have type POS_NN,
     * the the annotation type is POS
     *
     * @param aType
     *            the type sent from brat annotation as request while annotating
     */
    public static String getLabelPrefix(String aType)
    {
        String annotationType;
        if (Character.isDigit(aType.charAt(0))) {
            annotationType = aType.substring(0, aType.indexOf("_") + 1).replaceAll("[0-9]+", "");
        }
        else {
            annotationType = aType.substring(0, aType.indexOf("_") + 1);
        }
        return annotationType;
    }

    /**
     * Get the annotation type the way it is used in Brat visualization page (PREFIX+Type), such as
     * (POS_+NN)
     */
    public static String getQualifiedLabel(Tag aSelectedTag)
    {
        String annotationType = "";
        if (aSelectedTag.getTagSet().getType().getName().equals(AnnotationTypeConstant.POS)) {
            annotationType = AnnotationTypeConstant.POS_PREFIX + aSelectedTag.getName();
        }
        else if (aSelectedTag.getTagSet().getType().getName()
                .equals(AnnotationTypeConstant.DEPENDENCY)) {
            annotationType = AnnotationTypeConstant.DEP_PREFIX + aSelectedTag.getName();
        }
        else if (aSelectedTag.getTagSet().getType().getName()
                .equals(AnnotationTypeConstant.NAMEDENTITY)) {
            annotationType = AnnotationTypeConstant.NAMEDENTITY_PREFIX + aSelectedTag.getName();
        }
        else if (aSelectedTag.getTagSet().getType().getName()
                .equals(AnnotationTypeConstant.COREFRELTYPE)) {
            annotationType = AnnotationTypeConstant.COREFRELTYPE_PREFIX + aSelectedTag.getName();
        }
        else if (aSelectedTag.getTagSet().getType().getName()
                .equals(AnnotationTypeConstant.COREFERENCE)) {
            annotationType = AnnotationTypeConstant.COREFERENCE_PREFIX + aSelectedTag.getName();
        }
        return annotationType;
    }

    /**
     * Get label of annotation (arc or span value) If the request have type POS_NN, the the actual
     * annotation value is NN
     *
     * @param aQualifiedLabel
     *            the full label sent from brat annotation as request while annotating
     */
    public static String getLabel(String aQualifiedLabel)
    {
        String type;
        if (Character.isDigit(aQualifiedLabel.charAt(0))) {
            type = aQualifiedLabel.substring(aQualifiedLabel
                    .indexOf(AnnotationTypeConstant.PREFIX_SEPARATOR) + 1);
        }
        else {
            type = aQualifiedLabel.substring(aQualifiedLabel
                    .indexOf(AnnotationTypeConstant.PREFIX_SEPARATOR) + 1);
        }
        return type;
    }

    /**
     * Get the annotation layer name for arc {@link AnnotationType} such as
     * {@link AnnotationTypeConstant#DEPENDENCY} or {@link AnnotationTypeConstant#COREFERENCE} based
     * on the origin span type. This is assumed that an arc is drawn only from single span type such
     * as from {@link POS}. For Free Annotation type, the method should be changed.
     */
    public static String getArcLayerName(String aPrefix)
    {
        String layer = "";
        if (aPrefix.equals(AnnotationTypeConstant.POS_PREFIX)) {
            layer = AnnotationTypeConstant.DEPENDENCY;
        }
        else if (aPrefix.equals(AnnotationTypeConstant.COREFRELTYPE_PREFIX)) {
            layer = AnnotationTypeConstant.COREFERENCE;
        }
        return layer;
    }

    /**
     * Get the annotation layer name for span {@link AnnotationType} such as
     * {@link AnnotationTypeConstant#NAMEDENTITY} or {@link AnnotationTypeConstant#COREFRELTYPE}. If
     * this name is changed in the database, the {@link AnnotationTypeConstant} constants also
     * should be updated!
     */
    public static String getSpanLayerName(String aPrefix)
    {
        String layer = "";
        if (aPrefix.equals(AnnotationTypeConstant.POS_PREFIX)) {
            layer = AnnotationTypeConstant.POS;
        }
        else if (aPrefix.equals(AnnotationTypeConstant.NAMEDENTITY_PREFIX)) {
            layer = AnnotationTypeConstant.NAMEDENTITY;
        }
        else if (aPrefix.equals(AnnotationTypeConstant.COREFRELTYPE_PREFIX)) {
            layer = AnnotationTypeConstant.COREFRELTYPE;
        }
        else if (aPrefix.equals("")) {// no prefix for lemma
            layer = AnnotationTypeConstant.LEMMA;
        }
        return layer;
    }
}