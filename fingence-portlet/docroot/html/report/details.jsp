<%@page import="com.fingence.slayer.service.PortfolioItemServiceUtil"%>

<%@ include file="/html/report/init.jsp"%>

<%
	long portfolioId = ParamUtil.getLong(renderRequest, "portfolioId");
	Portfolio portfolio = PortfolioLocalServiceUtil.fetchPortfolio(portfolioId);
	String backURL = ParamUtil.getString(request, "backURL");
	String managerName = BridgeServiceUtil.getUserName(portfolio.getRelationshipManagerId());
	
	int portfolioItemCount = PortfolioItemServiceUtil.getPortfolioItems(portfolioId).size();
%>
	
<aui:fieldset>
	<aui:row>
		<aui:column columnWidth="25"><b>Investor</b></aui:column>
		<aui:column columnWidth="25"><b>Managed By</b></aui:column>
		<aui:column columnWidth="25"><b>Wealth Advisor</b></aui:column>
		<aui:column columnWidth="25"><b>Institution</b></aui:column>
	</aui:row>
	
	<aui:row>
		<aui:column columnWidth="25"><%= BridgeServiceUtil.getUserName(portfolio.getInvestorId()) %></aui:column>
		<aui:column columnWidth="25"><%= (Validator.isNull(managerName) ? "Not Assigned" : managerName) %></aui:column>
		<aui:column columnWidth="25"><%= BridgeServiceUtil.getUserName(portfolio.getWealthAdvisorId()) %></aui:column>
		<aui:column columnWidth="25"><%= BridgeServiceUtil.getOrganizationName(portfolio.getInstitutionId())  %></aui:column>
	</aui:row>	
</aui:fieldset>

<aui:container>
	<br/><a href="javascript:void(0);" onClick="javascript:updateItem(0, <%= portfolioId %>)">Add Asset</a><hr/>
	<div id="myDataTable"></div>
</aui:container>

<aui:script>
	<c:if test="<%= portfolioItemCount > 0 %>">
		AUI().ready(function(A) {
			Liferay.Service(
				'/fingence-portlet.myresult/get-my-results',
				{
					portfolioId : '<%= portfolioId %>'
				},
				function(data) {
					displayItemsGrid(data, '#myDataTable');
				}
			);
		});
	</c:if>
</aui:script>