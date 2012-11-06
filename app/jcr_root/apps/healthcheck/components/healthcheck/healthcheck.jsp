<%@page import="de.joerghoh.cq5.healthcheck.*,java.util.List" session="false"%>
<%@include file="/libs/foundation/global.jsp"%>
<%-- CQ5 healthcheck  component. --%>
<%
	HealthStatusService status = sling.getService(HealthStatusService.class);
	SystemHealthStatus overall = status.getOverallStatus();
	List<HealthStatus> details = overall.getDetails();
	String monitoringMessage = overall.getMonitoringMessage();
%>
<!DOCTYPE html>
<html>
<head>
	<title>cq5-healthcheck</title>
	<style media="screen" type="text/css">
		td.level_warn {
			background-color: yellow;
		}
		
		td.level_critical {
			background-color: red;
		}
		
		td.level_normal {
			background-color: green;
		}
	</style>
</head>
<body>
	<p>
		<b>Overall Status:</b>
		<%=overall.getStatus()%></p>
	<%
		if (!monitoringMessage.equals("")) {
	%><p>
		<b>Caution:</b>
		<%=monitoringMessage%></p>
	<%
		}
	%>
	<h1>Details</h1>
	<table border="1">
		<tr>
			<th>MBean</th>
			<th>Message</th>
			<th>Status</th>
		</tr>
		<%
			for (HealthStatus s : details) {
				String statusClass = "level_normal";
				if (s.getStatus() == HealthStatusProvider.WARN) {
					statusClass = "level_warn";
				}
				if (s.getStatus() == HealthStatusProvider.CRITICAL) {
					statusClass = "level_critical";
				}
		%>
		<tr>
			<td><%=s.getProvider()%></td>
			<td><%=s.getMessage()%>&nbsp;</td>
			<td class="<%=statusClass%>"><%=s.getStatusText()%></td>
		</tr>
		<%
			}
		%>

	</table>
</body>
</html>
