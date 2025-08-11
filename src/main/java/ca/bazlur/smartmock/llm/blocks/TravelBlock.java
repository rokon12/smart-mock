package ca.bazlur.smartmock.llm.blocks;

import ca.bazlur.smartmock.llm.ContextBlock;
import ca.bazlur.smartmock.llm.EndpointInfo;
import org.springframework.stereotype.Component;

@Component
public class TravelBlock implements ContextBlock {
  public String id() {
    return "travel.booking.v1";
  }

  public double score(EndpointInfo i) {
    double s = 0.0;
    String p = safe(i.path());
    String op = safe(i.operationId());
    String j = safe(i.jsonSchemaMinified());

    if (p.matches(".*\\b(flights?|hotels?|bookings?|reservations?|trips?|travel|destinations?)\\b.*")) s += 0.35;
    if (op.matches(".*(flight|hotel|booking|reservation|itinerary|travel).*")) s += 0.20;
    if (j.matches("(?s).*\\b(departure|arrival|checkin|checkout|passengers?|flightNumber|airline)\\b.*")) s += 0.35;
    if (j.matches("(?s).*\\b(destination|origin|duration|layover|class|seat)\\b.*")) s += 0.10;

    return Math.min(1.0, s);
  }

  public String render(EndpointInfo i) {
    String path = i.path().toLowerCase();
    
    StringBuilder sb = new StringBuilder();
    sb.append("TRAVEL CONTEXT:\n");
    
    if (path.contains("flight")) {
      sb.append("Example flight booking:\n");
      sb.append(FLIGHT_EXAMPLE);
    } else if (path.contains("hotel")) {
      sb.append("Example hotel booking:\n");
      sb.append(HOTEL_EXAMPLE);
    } else if (path.contains("itinerary")) {
      sb.append("Example itinerary:\n");
      sb.append(ITINERARY_EXAMPLE);
    } else {
      sb.append("Example booking:\n");
      sb.append(BOOKING_EXAMPLE);
    }
    
    sb.append("\nRules for travel data:\n");
    sb.append("- Use real airline codes (AA, UA, DL, BA, LH)\n");
    sb.append("- Use IATA airport codes (LAX, JFK, LHR, NRT)\n");
    sb.append("- Flight numbers like 'AA1234', 'UA567'\n");
    sb.append("- Realistic flight durations and layovers\n");
    sb.append("- Hotel names from major chains or boutique properties\n");
    sb.append("- Prices vary by class: economy $200-800, business $2000-5000\n");
    
    return sb.toString();
  }
  
  private static final String FLIGHT_EXAMPLE = """
      {
        "bookingReference": "ABC123",
        "flightNumber": "AA1234",
        "airline": "American Airlines",
        "departure": {
          "airport": "LAX",
          "city": "Los Angeles",
          "terminal": "4",
          "gate": "42B",
          "dateTime": "2024-03-15T08:30:00-07:00"
        },
        "arrival": {
          "airport": "JFK",
          "city": "New York",
          "terminal": "8",
          "gate": "12A",
          "dateTime": "2024-03-15T17:15:00-04:00"
        },
        "duration": "5h 45m",
        "aircraft": "Boeing 777-300ER",
        "class": "Economy",
        "seat": "24A",
        "passenger": {
          "firstName": "David",
          "lastName": "Chen",
          "frequentFlyer": "AA-1234567"
        },
        "price": {
          "base": 425.00,
          "tax": 67.50,
          "total": 492.50
        },
        "status": "confirmed"
      }
      """;
  
  private static final String HOTEL_EXAMPLE = """
      {
        "confirmationNumber": "HTL-2024-789456",
        "hotel": {
          "name": "Grand Hyatt New York",
          "address": "109 East 42nd Street, New York, NY 10017",
          "rating": 4.5,
          "phone": "+1-212-883-1234"
        },
        "checkIn": "2024-03-15",
        "checkOut": "2024-03-18",
        "nights": 3,
        "room": {
          "type": "Deluxe King",
          "number": "1247",
          "floor": 12,
          "amenities": ["WiFi", "Mini bar", "City view", "Work desk"]
        },
        "guests": 2,
        "ratePerNight": 289.00,
        "taxes": 51.75,
        "totalAmount": 918.75,
        "cancellationPolicy": "Free cancellation until 24 hours before check-in",
        "status": "confirmed"
      }
      """;
  
  private static final String BOOKING_EXAMPLE = """
      {
        "bookingId": "BKG-2024-123789",
        "tripName": "Spring Break NYC",
        "traveler": {
          "name": "Sarah Johnson",
          "email": "sarah.johnson@email.com",
          "phone": "+1-555-0123"
        },
        "startDate": "2024-03-15",
        "endDate": "2024-03-18",
        "totalCost": 1411.25,
        "components": [
          {
            "type": "flight",
            "reference": "ABC123",
            "amount": 492.50
          },
          {
            "type": "hotel",
            "reference": "HTL-2024-789456",
            "amount": 918.75
          }
        ],
        "paymentStatus": "paid",
        "bookingDate": "2024-01-20T10:30:00Z"
      }
      """;
  
  private static final String ITINERARY_EXAMPLE = """
      {
        "itineraryId": "ITN-2024-5678",
        "title": "European Adventure",
        "duration": "10 days",
        "segments": [
          {
            "day": 1,
            "date": "2024-06-01",
            "location": "Paris, France",
            "activities": ["Arrive at CDG", "Check in Hotel", "Evening Seine cruise"]
          },
          {
            "day": 2,
            "date": "2024-06-02",
            "location": "Paris, France",
            "activities": ["Eiffel Tower", "Louvre Museum", "Latin Quarter dinner"]
          }
        ]
      }
      """;

  private static String safe(String s) {
    return s == null ? "" : s.toLowerCase();
  }
}