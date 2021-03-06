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

package de.tudarmstadt.ukp.clarin.webanno.crowdflower;

import org.codehaus.jackson.annotate.JsonProperty;


/**
 * Represents one data row of the JSON data structure send to Crowdflower for NamedEntityTask1
 * @author Benjamin Milde
 *
 */
public class NamedEntityTask1Data
{
    public static final String FIELD_TEXT = "text";
    public static final String FIELD_GOLDEN = "_golden";
    public static final String FIELD_HIDDEN = "_hidden";
    public static final String FIELD_MARKERTEXT = "markertext";
    public static final String FIELD_MARKERTEXT_GOLD = "markertext_gold";
    public static final String FIELD_MARKERTEXT_GOLD_REASON = "markertext_gold_reason";
    public static final String FIELD_TYPES = "types";
    public static final String FIELD_DIFFICULTY = "_difficulty";
    public static final String FIELD_DOCUMENT = "document";
    public static final String FIELD_OFFSET = "offset";

    //text in which spans should be marked (usually in HTML spans)
    String text = "";
    //is this a gold element
    String _golden = "";
    //is this a hidden element: default no
    String _hidden = "FALSE";
    //position markers from JS of Crowdflower job, this is the result supplied by workers
    String markertext = "";
    //gold position markers if this is a gold element
    String markertext_gold = "";
    //reason why this is wrong display to worker if he missed his test question
    String markertext_gold_reason = "";
    //store the types of all NEs in this textfragment if this is a gold item
    String types = "";

    // from http://crowdflower.com/docs-gold: The _difficulty column should contain an integer between 1 and 100. The higher the number, the more difficult the Gold is considered, and it will be displayed later in a contributor's judgment session.
    int _difficulty = 1;

    //document name
    String document = "";

    int offset = 0;

    //constructor for normal data
    NamedEntityTask1Data(String text)
    {
        this.text = text;
    }

    @JsonProperty(FIELD_DOCUMENT)
    public String getDocument()
    {
        return document;
    }

    public void setDocument(String document)
    {
        this.document = document;
    }

    @JsonProperty(FIELD_OFFSET)
    public int getOffset()
    {
        return offset;
    }

    public void setOffset(int offset)
    {
        this.offset = offset;
    }


    @JsonProperty(FIELD_TEXT)
    public String getText()
    {
        return text;
    }
    public void setText(String text)
    {
        this.text = text;
    }

    @JsonProperty(FIELD_GOLDEN)
    public String get_golden()
    {
        return _golden;
    }
    public void set_golden(String _golden)
    {
        this._golden = _golden;
    }

    @JsonProperty(FIELD_HIDDEN)
    public String get_hidden()
    {
        return _hidden;
    }
    public void set_hidden(String _hidden)
    {
        this._golden = _hidden;
    }

    @JsonProperty(FIELD_MARKERTEXT)
    public String getMarkertext()
    {
        return markertext;
    }
    public void setMarkertext(String markertext)
    {
        this.markertext = markertext;
    }

    @JsonProperty(FIELD_MARKERTEXT_GOLD)
    public String getMarkertext_gold()
    {
        return markertext_gold;
    }
    public void setMarkertext_gold(String markertext_gold)
    {
        this.markertext_gold = markertext_gold;
    }

    @JsonProperty(FIELD_MARKERTEXT_GOLD_REASON)
    public String getMarkertext_gold_reason()
    {
        return markertext_gold_reason;
    }
    public void setMarkertext_gold_reason(String markertext_gold_reason)
    {
        this.markertext_gold_reason = markertext_gold_reason;
    }

    @JsonProperty(FIELD_TYPES)
    public String getTypes()
    {
        return types;
    }

    public void setTypes(String types)
    {
        this.types = types;
    }

    @JsonProperty(FIELD_DIFFICULTY)
    public int get_difficulty()
    {
        return _difficulty;
    }

    public void set_difficulty(int _difficulty)
    {
        this._difficulty = _difficulty;
    }
}
