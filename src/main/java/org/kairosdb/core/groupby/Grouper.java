/*
 * Copyright 2013 Proofpoint Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.kairosdb.core.groupby;

import org.kairosdb.core.DataPoint;
import org.kairosdb.core.datastore.DataPointGroup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Groups data points based on a list of GroupBys.
 */
public class Grouper
{
	Map<List<Integer>, Group> groupIdsToGroup = new LinkedHashMap<List<Integer>, Group>();

	/**
	 * Groups data points by group bys.
	 *
	 * @param groupBys list of group bys
	 * @param dataPointGroupList list of data point groups to group
	 * @return list of data point groups
	 */
	public List<DataPointGroup> group(List<GroupBy> groupBys, List<DataPointGroup> dataPointGroupList) throws IOException
	{
		if (groupBys.size() < 1)
			return dataPointGroupList;


		for (DataPointGroup dataPointGroup : dataPointGroupList)
		{
			Map<String, String> tags = getTags(dataPointGroup);

			while (dataPointGroup.hasNext())
			{
				DataPoint dataPoint = dataPointGroup.next();

				List<Integer> groupIds = new ArrayList<Integer>();
				List<GroupByResult> results = new ArrayList<GroupByResult>();
				for (GroupBy groupBy : groupBys)
				{
					int groupId = groupBy.getGroupId(dataPoint, tags);
					groupIds.add(groupId);
					results.add(groupBy.getGroupByResult(groupId));
				}

				// add to group
				Group group = getGroup(dataPointGroup, groupIds, results);
				group.addDataPoint(dataPoint);
//				group.addTags(dataPointGroup); // todo what to do about tags. This causes the grouping to be very, very slow.
			}
		}

		List<DataPointGroup> dataPointGroups = new ArrayList<DataPointGroup>();
		for (Group group : groupIdsToGroup.values())
		{
			dataPointGroups.add(group.getDataPointGroup());
		}

		return dataPointGroups;
	}

	private Group getGroup(DataPointGroup dataPointGroup, List<Integer> groupIds, List<GroupByResult> results) throws IOException
	{
		Group group = groupIdsToGroup.get(groupIds);
		if (group == null)
		{
			group = Group.createGroup(dataPointGroup, groupIds, results);
			groupIdsToGroup.put(groupIds, group);
		}
		return group;
	}

	private Map<String, String> getTags(DataPointGroup dataPointGroup)
	{
		Map<String, String> map = new LinkedHashMap<String, String>();
		for (String tagName : dataPointGroup.getTagNames())
		{
			map.put(tagName, dataPointGroup.getTagValues(tagName).iterator().next());
		}

		return map;
	}

}