package ca.bazlur.smartmock.llm.blocks;

import ca.bazlur.smartmock.llm.ContextBlock;
import ca.bazlur.smartmock.llm.EndpointInfo;
import org.springframework.stereotype.Component;

@Component
public class AnalyticsBlock implements ContextBlock {
  public String id() {
    return "analytics.metrics.v1";
  }

  public double score(EndpointInfo i) {
    double s = 0.0;
    String p = safe(i.path());
    String op = safe(i.operationId());
    String j = safe(i.jsonSchemaMinified());

    if (p.matches(".*\\b(analytics?|metrics?|stats?|reports?|dashboards?|insights?|kpis?)\\b.*")) s += 0.35;
    if (op.matches(".*(analytics|metrics|statistics|report|dashboard|performance).*")) s += 0.20;
    if (j.matches("(?s).*\\b(pageViews|sessions|bounceRate|conversion|revenue|visitors)\\b.*")) s += 0.35;
    if (j.matches("(?s).*\\b(timeRange|dimension|metric|segment|chart)\\b.*")) s += 0.10;

    return Math.min(1.0, s);
  }

  public String render(EndpointInfo i) {
    String path = i.path().toLowerCase();
    
    StringBuilder sb = new StringBuilder();
    sb.append("ANALYTICS CONTEXT:\n");
    
    if (path.contains("dashboard")) {
      sb.append("Example dashboard data:\n");
      sb.append(DASHBOARD_EXAMPLE);
    } else if (path.contains("report")) {
      sb.append("Example analytics report:\n");
      sb.append(REPORT_EXAMPLE);
    } else {
      sb.append("Example metrics:\n");
      sb.append(METRICS_EXAMPLE);
    }
    
    sb.append("\nRules for analytics data:\n");
    sb.append("- Use realistic metric values with appropriate ranges\n");
    sb.append("- Include time series data with ISO-8601 timestamps\n");
    sb.append("- Percentages 0-100 with decimals (e.g., 67.3%)\n");
    sb.append("- Include comparisons (vs previous period)\n");
    sb.append("- Common metrics: CTR, conversion rate, bounce rate, session duration\n");
    sb.append("- Aggregate data by day, week, month, quarter\n");
    
    return sb.toString();
  }
  
  private static final String METRICS_EXAMPLE = """
      {
        "period": {
          "start": "2024-01-01",
          "end": "2024-01-31"
        },
        "metrics": {
          "totalVisitors": 125678,
          "uniqueVisitors": 89234,
          "pageViews": 456789,
          "sessions": 98765,
          "avgSessionDuration": 245,
          "bounceRate": 42.7,
          "conversionRate": 3.2
        },
        "comparison": {
          "previousPeriod": {
            "totalVisitors": 115234,
            "change": 9.1,
            "trend": "up"
          }
        },
        "topPages": [
          {
            "path": "/products",
            "views": 45678,
            "avgTimeOnPage": 125
          },
          {
            "path": "/home",
            "views": 38900,
            "avgTimeOnPage": 45
          }
        ],
        "devices": {
          "desktop": 45.2,
          "mobile": 48.3,
          "tablet": 6.5
        }
      }
      """;
  
  private static final String DASHBOARD_EXAMPLE = """
      {
        "dashboardId": "dash-2024-main",
        "title": "Executive Dashboard",
        "lastUpdated": "2024-01-25T16:00:00Z",
        "kpis": [
          {
            "name": "Revenue",
            "value": 2456789,
            "target": 2500000,
            "achievement": 98.3,
            "trend": "up",
            "changePercent": 12.5
          },
          {
            "name": "Active Users",
            "value": 45678,
            "target": 50000,
            "achievement": 91.4,
            "trend": "stable",
            "changePercent": 1.2
          }
        ],
        "charts": [
          {
            "type": "line",
            "title": "Daily Revenue",
            "data": [
              {"date": "2024-01-20", "value": 78900},
              {"date": "2024-01-21", "value": 82300},
              {"date": "2024-01-22", "value": 79500},
              {"date": "2024-01-23", "value": 91200},
              {"date": "2024-01-24", "value": 88700}
            ]
          }
        ]
      }
      """;
  
  private static final String REPORT_EXAMPLE = """
      {
        "reportId": "RPT-2024-Q1",
        "title": "Q1 2024 Performance Report",
        "generatedAt": "2024-01-25T09:00:00Z",
        "summary": {
          "revenue": {
            "total": 8456789,
            "growth": 15.3,
            "byProduct": {
              "productA": 3456789,
              "productB": 2890123,
              "productC": 2110877
            }
          },
          "customers": {
            "new": 12456,
            "returning": 34567,
            "churnRate": 5.2,
            "lifetime": 67890
          },
          "performance": {
            "siteSpeed": 2.3,
            "uptime": 99.95,
            "errorRate": 0.02
          }
        },
        "trends": [
          {
            "metric": "Monthly Recurring Revenue",
            "current": 745000,
            "previous": 680000,
            "change": 9.6
          },
          {
            "metric": "Customer Acquisition Cost",
            "current": 125,
            "previous": 145,
            "change": -13.8
          }
        ],
        "recommendations": [
          "Focus on mobile optimization - 48% of traffic",
          "Improve page load times for conversion pages",
          "Increase marketing spend on high-performing channels"
        ]
      }
      """;

  private static String safe(String s) {
    return s == null ? "" : s.toLowerCase();
  }
}