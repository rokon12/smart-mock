package ca.bazlur.smartmock.llm.blocks;

import ca.bazlur.smartmock.llm.ContextBlock;
import ca.bazlur.smartmock.llm.EndpointInfo;
import org.springframework.stereotype.Component;

@Component
public class EducationBlock implements ContextBlock {
  public String id() {
    return "education.learning.v1";
  }

  public double score(EndpointInfo i) {
    double s = 0.0;
    String p = safe(i.path());
    String op = safe(i.operationId());
    String j = safe(i.jsonSchemaMinified());

    if (p.matches(".*\\b(courses?|students?|teachers?|lessons?|enrollments?|grades?|classes?)\\b.*")) s += 0.35;
    if (op.matches(".*(course|student|enrollment|lesson|grade|assignment|exam).*")) s += 0.20;
    if (j.matches("(?s).*\\b(courseId|studentId|grade|credits|semester|curriculum|syllabus)\\b.*")) s += 0.35;
    if (j.matches("(?s).*\\b(duration|startDate|endDate|prerequisites|difficulty)\\b.*")) s += 0.10;

    return Math.min(1.0, s);
  }

  public String render(EndpointInfo i) {
    String path = i.path().toLowerCase();
    
    StringBuilder sb = new StringBuilder();
    sb.append("EDUCATION CONTEXT:\n");
    
    if (path.contains("course")) {
      sb.append("Example course:\n");
      sb.append(COURSE_EXAMPLE);
    } else if (path.contains("student")) {
      sb.append("Example student:\n");
      sb.append(STUDENT_EXAMPLE);
    } else if (path.contains("enrollment")) {
      sb.append("Example enrollment:\n");
      sb.append(ENROLLMENT_EXAMPLE);
    } else {
      sb.append("Example course:\n");
      sb.append(COURSE_EXAMPLE);
    }
    
    sb.append("\nRules for educational data:\n");
    sb.append("- Use real course names like 'Introduction to Computer Science', 'Advanced Calculus'\n");
    sb.append("- Student IDs like 'STU-2024-1234' or standard university format\n");
    sb.append("- Grades: A+, A, B+, B, C+, C, D, F or percentage (0-100)\n");
    sb.append("- Credits typically 1-4, duration in weeks/hours\n");
    sb.append("- Include realistic prerequisites and learning outcomes\n");
    
    return sb.toString();
  }
  
  private static final String COURSE_EXAMPLE = """
      {
        "courseId": "CS-101",
        "title": "Introduction to Computer Science",
        "description": "Fundamental concepts of programming, algorithms, and computational thinking",
        "credits": 3,
        "duration": "16 weeks",
        "instructor": "Prof. Jennifer Williams",
        "department": "Computer Science",
        "level": "Undergraduate",
        "prerequisites": [],
        "capacity": 150,
        "enrolled": 142,
        "schedule": {
          "days": ["Monday", "Wednesday", "Friday"],
          "time": "10:00-10:50",
          "room": "Tech Building 201"
        },
        "syllabus": "https://university.edu/syllabus/CS101.pdf",
        "startDate": "2024-01-15",
        "endDate": "2024-05-10"
      }
      """;
  
  private static final String STUDENT_EXAMPLE = """
      {
        "studentId": "STU-2024-7834",
        "firstName": "Michael",
        "lastName": "Thompson",
        "email": "m.thompson@university.edu",
        "major": "Computer Science",
        "minor": "Mathematics",
        "year": 3,
        "gpa": 3.67,
        "creditsCompleted": 72,
        "creditsRequired": 120,
        "enrollmentStatus": "active",
        "academicStanding": "good",
        "advisor": "Dr. Robert Garcia",
        "expectedGraduation": "2025-05"
      }
      """;
  
  private static final String ENROLLMENT_EXAMPLE = """
      {
        "enrollmentId": "ENR-2024-45612",
        "studentId": "STU-2024-7834",
        "courseId": "CS-101",
        "semester": "Spring 2024",
        "status": "enrolled",
        "enrollmentDate": "2024-01-08T14:30:00Z",
        "grade": null,
        "attendance": 95,
        "assignments": [
          {
            "name": "Project 1",
            "grade": 92,
            "weight": 0.25
          },
          {
            "name": "Midterm Exam",
            "grade": 88,
            "weight": 0.30
          }
        ],
        "currentGrade": 90.2
      }
      """;

  private static String safe(String s) {
    return s == null ? "" : s.toLowerCase();
  }
}