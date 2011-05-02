/* 
 * The Fascinator - Common Library
 * Copyright (C) 2008-2009 University of Southern Queensland
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package au.edu.usq.fascinator.common.jaxb;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * JAXB adapter for unmarshalling the following XML structure into a map:
 * 
 * <pre>
 * &lt;fields&gt;
 *   &lt;field name=&quot;key1&quot;&gt;value1&lt;/elem&gt;
 *   &lt;field name=&quot;key2&quot;&gt;value2&lt;/elem&gt;
 *   &lt;field name=&quot;key3&quot;&gt;value3&lt;/elem&gt;
 * &lt;/fields&gt;
 * </pre>
 * 
 * @author Oliver Lucido
 */
public class MapAdapter extends
        XmlAdapter<MapEntryListType, Map<String, String>> {

    @Override
    public MapEntryListType marshal(Map<String, String> map) throws Exception {
        return new MapEntryListType(map);
    }

    @Override
    public Map<String, String> unmarshal(MapEntryListType entryList)
            throws Exception {
        Map<String, String> map = new HashMap<String, String>();
        for (MapEntryType entry : entryList.getEntries()) {
            map.put(entry.getName(), entry.getValue());
        }
        return map;
    }
}
