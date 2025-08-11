package ca.bazlur.smartmock.llm.blocks;

import ca.bazlur.smartmock.llm.ContextBlock;
import ca.bazlur.smartmock.llm.EndpointInfo;
import org.springframework.stereotype.Component;

@Component
public class RealEstateBlock implements ContextBlock {
  public String id() {
    return "realestate.property.v1";
  }

  public double score(EndpointInfo i) {
    double s = 0.0;
    String p = safe(i.path());
    String op = safe(i.operationId());
    String j = safe(i.jsonSchemaMinified());

    if (p.matches(".*\\b(properties?|listings?|houses?|apartments?|rentals?|real-?estate)\\b.*")) s += 0.35;
    if (op.matches(".*(property|listing|house|apartment|rental|mortgage).*")) s += 0.20;
    if (j.matches("(?s).*\\b(bedrooms?|bathrooms?|squareFeet|price|address|zipCode|mls)\\b.*")) s += 0.35;
    if (j.matches("(?s).*\\b(yearBuilt|propertyType|garage|amenities|hoa)\\b.*")) s += 0.10;

    return Math.min(1.0, s);
  }

  public String render(EndpointInfo i) {
    String path = i.path().toLowerCase();
    
    StringBuilder sb = new StringBuilder();
    sb.append("REAL ESTATE CONTEXT:\n");
    
    if (path.contains("listing")) {
      sb.append("Example property listing:\n");
      sb.append(LISTING_EXAMPLE);
    } else if (path.contains("rental")) {
      sb.append("Example rental property:\n");
      sb.append(RENTAL_EXAMPLE);
    } else {
      sb.append("Example property:\n");
      sb.append(PROPERTY_EXAMPLE);
    }
    
    sb.append("\nRules for real estate data:\n");
    sb.append("- Use realistic addresses with actual city names\n");
    sb.append("- MLS numbers like 'MLS-2024-789456'\n");
    sb.append("- Prices appropriate for property type ($200K-$2M for houses)\n");
    sb.append("- Square footage: 500-5000+ for residential\n");
    sb.append("- Include realistic features and amenities\n");
    sb.append("- Property types: single-family, condo, townhouse, multi-family\n");
    
    return sb.toString();
  }
  
  private static final String PROPERTY_EXAMPLE = """
      {
        "propertyId": "PROP-2024-5678",
        "mlsNumber": "MLS-2024-789456",
        "address": {
          "street": "1425 Oak Ridge Drive",
          "city": "Austin",
          "state": "TX",
          "zipCode": "78731",
          "country": "USA"
        },
        "price": 675000,
        "propertyType": "single-family",
        "bedrooms": 4,
        "bathrooms": 2.5,
        "squareFeet": 2850,
        "lotSize": 0.35,
        "yearBuilt": 2018,
        "garage": 2,
        "features": [
          "Granite countertops",
          "Hardwood floors",
          "Stainless steel appliances",
          "Walk-in closets",
          "Covered patio"
        ],
        "hoa": {
          "fee": 125,
          "frequency": "monthly"
        },
        "taxAssessment": 8500,
        "status": "for-sale",
        "listingDate": "2024-01-15",
        "virtualTour": "https://tours.example.com/PROP-2024-5678"
      }
      """;
  
  private static final String LISTING_EXAMPLE = """
      {
        "listingId": "LST-2024-1234",
        "propertyId": "PROP-2024-5678",
        "listPrice": 675000,
        "originalPrice": 695000,
        "daysOnMarket": 45,
        "status": "active",
        "listingAgent": {
          "name": "Jennifer Martinez",
          "agency": "Premier Realty Group",
          "phone": "+1-512-555-0142",
          "email": "j.martinez@premierrealty.com"
        },
        "openHouse": [
          {
            "date": "2024-02-10",
            "startTime": "14:00",
            "endTime": "16:00"
          }
        ],
        "priceHistory": [
          {
            "date": "2024-01-15",
            "price": 695000,
            "event": "Listed"
          },
          {
            "date": "2024-02-01",
            "price": 675000,
            "event": "Price reduced"
          }
        ]
      }
      """;
  
  private static final String RENTAL_EXAMPLE = """
      {
        "rentalId": "RNT-2024-9876",
        "propertyId": "PROP-2024-3456",
        "monthlyRent": 2500,
        "securityDeposit": 2500,
        "leaseTerm": "12 months",
        "availableDate": "2024-03-01",
        "petPolicy": {
          "allowed": true,
          "deposit": 500,
          "monthlyFee": 50,
          "restrictions": "Max 2 pets, under 50 lbs"
        },
        "utilities": {
          "included": ["Water", "Trash"],
          "tenantPays": ["Electricity", "Gas", "Internet"]
        },
        "amenities": [
          "Pool",
          "Gym",
          "Parking",
          "In-unit washer/dryer"
        ],
        "requirements": {
          "minCreditScore": 650,
          "incomeRequirement": "3x rent",
          "backgroundCheck": true
        }
      }
      """;

  private static String safe(String s) {
    return s == null ? "" : s.toLowerCase();
  }
}