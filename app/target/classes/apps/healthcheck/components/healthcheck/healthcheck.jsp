<%--

  CQ5 healthcheck  component.

  healthcheck component

--%><%@page
	import="
	de.joerghoh.cq5.healthcheck.HealthStatusService,
	de.joerghoh.cq5.healthcheck.HealthStatusProvider,
	de.joerghoh.cq5.healthcheck.OverallHealthStatus,
	de.joerghoh.cq5.healthcheck.HealthStatus,
	java.util.List"
%><%@include
	file="/libs/foundation/global.jsp"%>
<%
%><%@page session="false"%><%
%>
<%
	
	HealthStatusService status = sling.getService(HealthStatusService.class);
	OverallHealthStatus overall = status.getOverallStatus();
	List<HealthStatus> details = overall.getDetails();

%>
<html>
<body>
	<b>Overall Status:</b>
	<%= overall.getStatus() %>
	<h1>Details</h1>
	<table>
		<tr><th>Responsible</th><th>Message</th><th>Status</th></tr>
		<% for (HealthStatus s: details) { 
			String statusColor = "green";
			if (s.getStatus() == HealthStatusProvider.HS_WARN) {
				statusColor = "yellow";
			}
			if (s.getStatus() == HealthStatusProvider.HS_ERROR) {
				statusColor = "red";
			}
		%>
		<tr><td><%= s.getProvider() %></td><td><%= s.getMessage() %></td><td style="background-color:<%= statusColor %> "><%= s.getStatusText() %></td></tr>
		<%} %>
	
	</table>
</body>
</html>
