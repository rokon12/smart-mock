package ca.bazlur.smartmock.llm.blocks;

import ca.bazlur.smartmock.llm.ContextBlock;
import ca.bazlur.smartmock.llm.EndpointInfo;
import org.springframework.stereotype.Component;

@Component
public class HealthcareBlock implements ContextBlock {
  public String id() {
    return "healthcare.medical.v1";
  }

  public double score(EndpointInfo i) {
    double s = 0.0;
    String p = safe(i.path());
    String op = safe(i.operationId());
    String j = safe(i.jsonSchemaMinified());

    if (p.matches(".*\\b(patients?|doctors?|appointments?|medical|health|clinic|hospital)\\b.*")) s += 0.35;
    if (op.matches(".*(patient|doctor|appointment|medical|prescription|diagnosis).*")) s += 0.20;
    if (j.matches("(?s).*\\b(diagnosis|symptoms?|medication|prescription|dosage|allergy|bloodType)\\b.*")) s += 0.35;
    if (j.matches("(?s).*\\b(patientId|doctorId|medicalRecordNumber|insuranceId)\\b.*")) s += 0.10;

    return Math.min(1.0, s);
  }

  public String render(EndpointInfo i) {
    String path = i.path().toLowerCase();
    boolean isSingle = path.matches(".*/(\\{[^}]+\\}|:\\w+|\\d+).*");
    
    StringBuilder sb = new StringBuilder();
    sb.append("HEALTHCARE CONTEXT:\n");
    
    if (path.contains("patient")) {
      sb.append("Example patient record:\n");
      sb.append(PATIENT_EXAMPLE);
    } else if (path.contains("appointment")) {
      sb.append("Example appointment:\n");
      sb.append(APPOINTMENT_EXAMPLE);
    } else {
      sb.append("Example medical record:\n");
      sb.append(MEDICAL_RECORD_EXAMPLE);
    }
    
    sb.append("\nRules for healthcare data:\n");
    sb.append("- Use realistic medical terms and ICD-10 codes where applicable\n");
    sb.append("- Patient IDs like 'PAT-123456' or 'MRN-789012'\n");
    sb.append("- Include proper medical units (mg, ml, bpm, mmHg)\n");
    sb.append("- Respect HIPAA - use realistic but anonymous data\n");
    sb.append("- Dates in ISO-8601, appointments in future unless historical\n");
    
    return sb.toString();
  }
  
  private static final String PATIENT_EXAMPLE = """
      {
        "patientId": "PAT-789456",
        "mrn": "MRN-2024-1532",
        "firstName": "Robert",
        "lastName": "Martinez",
        "dateOfBirth": "1975-06-15",
        "gender": "male",
        "bloodType": "O+",
        "allergies": ["Penicillin", "Peanuts"],
        "currentMedications": [
          {
            "name": "Lisinopril",
            "dosage": "10mg",
            "frequency": "Once daily"
          }
        ],
        "primaryPhysician": "Dr. Sarah Chen",
        "insuranceProvider": "Blue Cross Blue Shield",
        "insuranceId": "BCB-445789632",
        "lastVisit": "2024-01-10T09:30:00Z"
      }
      """;
  
  private static final String APPOINTMENT_EXAMPLE = """
      {
        "appointmentId": "APT-2024-8934",
        "patientId": "PAT-789456",
        "doctorId": "DOC-234",
        "appointmentDate": "2024-02-15T14:30:00Z",
        "type": "Follow-up",
        "department": "Cardiology",
        "reason": "Blood pressure monitoring",
        "status": "confirmed",
        "duration": 30,
        "location": "Building A, Room 302"
      }
      """;
  
  private static final String MEDICAL_RECORD_EXAMPLE = """
      {
        "recordId": "MED-2024-1122",
        "patientId": "PAT-789456",
        "visitDate": "2024-01-10T09:30:00Z",
        "chiefComplaint": "Chest discomfort",
        "vitals": {
          "bloodPressure": "130/85 mmHg",
          "heartRate": 78,
          "temperature": 98.6,
          "weight": 180,
          "height": 70
        },
        "diagnosis": [
          {
            "code": "I10",
            "description": "Essential hypertension"
          }
        ],
        "prescriptions": [
          {
            "medication": "Amlodipine",
            "dosage": "5mg",
            "frequency": "Once daily",
            "duration": "90 days"
          }
        ]
      }
      """;

  private static String safe(String s) {
    return s == null ? "" : s.toLowerCase();
  }
}