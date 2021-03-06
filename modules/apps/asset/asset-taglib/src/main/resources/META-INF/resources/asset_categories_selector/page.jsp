<%--
/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
--%>

<%@ include file="/asset_categories_selector/init.jsp" %>

<%
String randomNamespace = PortalUtil.generateRandomKey(request, "taglib_ui_asset_categories_selector_page") + StringPool.UNDERLINE;

List<String[]> categoryIdsTitles = (List<String[]>)request.getAttribute("liferay-asset:asset-categories-selector:categoryIdsTitles");
String className = (String)request.getAttribute("liferay-asset:asset-categories-selector:className");
long classTypePK = GetterUtil.getLong((String)request.getAttribute("liferay-asset:asset-categories-selector:classTypePK"));
String eventName = (String)request.getAttribute("liferay-asset:asset-categories-selector:eventName");
String hiddenInput = (String)request.getAttribute("liferay-asset:asset-categories-selector:hiddenInput");
PortletURL portletURL = (PortletURL)request.getAttribute("liferay-asset:asset-categories-selector:portletURL");
boolean showRequiredLabel = GetterUtil.getBoolean((String)request.getAttribute("liferay-asset:asset-categories-selector:showRequiredLabel"), true);
boolean singleSelect = GetterUtil.getBoolean((String)request.getAttribute("liferay-asset:asset-categories-selector:singleSelect"));
List<AssetVocabulary> vocabularies = (List<AssetVocabulary>)request.getAttribute("liferay-asset:asset-categories-selector:vocabularies");

int maxEntries = GetterUtil.getInteger(PropsUtil.get(PropsKeys.ASSET_CATEGORIES_SELECTOR_MAX_ENTRIES));
%>

<c:choose>
	<c:when test="<%= Validator.isNotNull(className) %>">

		<%
		for (int i = 0; i < vocabularies.size(); i++) {
			AssetVocabulary vocabulary = vocabularies.get(i);
		%>

			<span class="field-content">
				<label id="<%= randomNamespace %>assetCategoriesLabel_<%= vocabulary.getVocabularyId() %>">
					<%= vocabulary.getTitle(locale) %>

					<c:if test="<%= vocabulary.getGroupId() != themeDisplay.getSiteGroupId() %>">

						<%
						Group vocabularyGroup = GroupLocalServiceUtil.getGroup(vocabulary.getGroupId());
						%>

						(<%= HtmlUtil.escape(vocabularyGroup.getDescriptiveName(locale)) %>)
					</c:if>

					<c:if test="<%= vocabulary.isRequired(PortalUtil.getClassNameId(className), classTypePK) && showRequiredLabel %>">
						<span class="icon-asterisk text-warning">
							<span class="hide-accessible"><liferay-ui:message key="required" /></span>
						</span>
					</c:if>
				</label>

				<div class="lfr-tags-selector-content" id="<%= randomNamespace %>assetCategoriesSelector_<%= vocabulary.getVocabularyId() %>">
					<aui:input name="<%= hiddenInput + StringPool.UNDERLINE + vocabulary.getVocabularyId() %>" type="hidden" />
				</div>
			</span>

			<%
			String[] categoryIdsTitle = categoryIdsTitles.get(i);
			%>

			<aui:script use="liferay-asset-taglib-categories-selector">
				new Liferay.AssetTaglibCategoriesSelector(
					{
						categoryIds: '<%= categoryIdsTitle[0] %>',
						categoryTitles: '<%= HtmlUtil.escapeJS(categoryIdsTitle[1]) %>',
						contentBox: '#<%= randomNamespace %>assetCategoriesSelector_<%= vocabulary.getVocabularyId() %>',
						eventName: '<%= eventName %>',
						hiddenInput: '#<portlet:namespace /><%= hiddenInput + StringPool.UNDERLINE + vocabulary.getVocabularyId() %>',
						instanceVar: '<portlet:namespace />',
						labelNode: '#<%= randomNamespace %>assetCategoriesLabel_<%= vocabulary.getVocabularyId() %>',
						maxEntries: <%= maxEntries %>,
						moreResultsLabel: '<liferay-ui:message key="load-more-results" />',

						<c:if test="<%= portletURL != null %>">
							portletURL: '<%= portletURL.toString() %>',
						</c:if>

						singleSelect: <%= !vocabulary.isMultiValued() %>,
						title: '<liferay-ui:message arguments="<%= vocabulary.getTitle(locale) %>" key="select-x" translateArguments="<%= false %>" />',
						vocabularyIds: '<%= String.valueOf(vocabulary.getVocabularyId()) %>'
					}
				).render();
			</aui:script>

		<%
		}
		%>

	</c:when>
	<c:otherwise>

		<%
		String[] categoryIdsTitle = categoryIdsTitles.get(0);
		%>

		<div class="lfr-tags-selector-content" id="<%= randomNamespace %>assetCategoriesSelector">
			<aui:input name="<%= hiddenInput %>" type="hidden" />
		</div>

		<aui:script use="liferay-asset-taglib-categories-selector">
			new Liferay.AssetTaglibCategoriesSelector(
				{
					categoryIds: '<%= categoryIdsTitle[0] %>',
					categoryTitles: '<%= HtmlUtil.escapeJS(categoryIdsTitle[1]) %>',
					contentBox: '#<%= randomNamespace %>assetCategoriesSelector',
					eventName: '<%= eventName %>',
					hiddenInput: '#<portlet:namespace /><%= hiddenInput %>',
					instanceVar: '<portlet:namespace />',
					maxEntries: <%= maxEntries %>,
					moreResultsLabel: '<liferay-ui:message key="load-more-results" />',

					<c:if test="<%= portletURL != null %>">
						portletURL: '<%= portletURL.toString() %>',
					</c:if>

					singleSelect: <%= singleSelect %>,
					vocabularyIds: '<%= ListUtil.toString(vocabularies, "vocabularyId") %>'
				}
			).render();
		</aui:script>
	</c:otherwise>
</c:choose>