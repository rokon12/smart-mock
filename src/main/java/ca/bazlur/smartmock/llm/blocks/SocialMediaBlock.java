package ca.bazlur.smartmock.llm.blocks;

import ca.bazlur.smartmock.llm.ContextBlock;
import ca.bazlur.smartmock.llm.EndpointInfo;
import org.springframework.stereotype.Component;

@Component
public class SocialMediaBlock implements ContextBlock {
  public String id() {
    return "social.media.v1";
  }

  public double score(EndpointInfo i) {
    double s = 0.0;
    String p = safe(i.path());
    String op = safe(i.operationId());
    String j = safe(i.jsonSchemaMinified());

    if (p.matches(".*\\b(posts?|comments?|likes?|followers?|feeds?|profiles?|stories?|messages?)\\b.*"))
      s += 0.35;
    if (op.matches(".*(post|comment|like|follow|share|profile|feed|story).*"))
      s += 0.20;
    if (j.matches("(?s).*\\b(username|followers|following|likes|shares|views|hashtags?)\\b.*"))
      s += 0.35;
    if (j.matches("(?s).*\\b(profilePicture|bio|verified|engagement|reach)\\b.*"))
      s += 0.10;

    return Math.min(1.0, s);
  }

  public String render(EndpointInfo i) {
    String path = i.path().toLowerCase();

    StringBuilder sb = new StringBuilder();
    sb.append("SOCIAL MEDIA CONTEXT:\n");

    if (path.contains("post")) {
      sb.append("Example social post:\n");
      sb.append(POST_EXAMPLE);
    } else if (path.contains("profile")) {
      sb.append("Example user profile:\n");
      sb.append(PROFILE_EXAMPLE);
    } else if (path.contains("comment")) {
      sb.append("Example comment:\n");
      sb.append(COMMENT_EXAMPLE);
    } else {
      sb.append("Example feed item:\n");
      sb.append(FEED_EXAMPLE);
    }

    sb.append("\nRules for social media data:\n");
    sb.append("- Usernames like @johndoe, @tech_guru, @creative_mind\n");
    sb.append("- Realistic engagement numbers (likes: 10-100K, views: 100-1M)\n");
    sb.append("- Include trending hashtags #technology #motivation #photography\n");
    sb.append("- Post types: text, image, video, story, reel\n");
    sb.append("- Timestamps recent and in ISO-8601\n");
    sb.append("- Verified accounts for popular users\n");

    return sb.toString();
  }

  private static final String POST_EXAMPLE = """
      {
        "postId": "post-2024-789456",
        "userId": "usr-123456",
        "username": "@tech_enthusiast",
        "content": "Just launched my new app! Excited to share this journey with you all 🚀",
        "mediaType": "image",
        "mediaUrl": "https://cdn.social.com/images/post-789456.jpg",
        "hashtags": ["#startup", "#appdevelopment", "#entrepreneur"],
        "mentions": ["@startup_hub", "@dev_community"],
        "likes": 1247,
        "comments": 89,
        "shares": 45,
        "views": 15670,
        "engagement": {
          "rate": 8.2,
          "reach": 25000,
          "impressions": 35000
        },
        "postedAt": "2024-01-25T14:30:00Z",
        "visibility": "public"
      }
      """;

  private static final String PROFILE_EXAMPLE = """
      {
        "userId": "usr-123456",
        "username": "@sarah_creative",
        "displayName": "Sarah Martinez",
        "bio": "Digital artist | Coffee enthusiast | Spreading positivity ✨",
        "profilePicture": "https://cdn.social.com/profiles/sarah_creative.jpg",
        "coverPhoto": "https://cdn.social.com/covers/sarah_creative.jpg",
        "verified": true,
        "followers": 45678,
        "following": 892,
        "posts": 1234,
        "joinedDate": "2020-03-15",
        "location": "San Francisco, CA",
        "website": "https://sarahcreative.com",
        "categories": ["Art", "Lifestyle", "Photography"],
        "statistics": {
          "avgLikesPerPost": 2340,
          "avgCommentsPerPost": 156,
          "engagementRate": 5.2,
          "monthlyReach": 250000
        }
      }
      """;

  private static final String COMMENT_EXAMPLE = """
      {
        "commentId": "cmt-2024-5678",
        "postId": "post-2024-789456",
        "userId": "usr-987654",
        "username": "@mike_developer",
        "content": "Congratulations! This looks amazing! Can't wait to try it out 🎉",
        "likes": 45,
        "replies": 3,
        "timestamp": "2024-01-25T15:45:00Z",
        "edited": false
      }
      """;

  private static final String FEED_EXAMPLE = """
      {
        "feedItems": [
          {
            "type": "post",
            "postId": "post-2024-111",
            "author": "@fitness_guru",
            "content": "Morning workout complete! 💪",
            "mediaType": "video",
            "thumbnail": "https://cdn.social.com/thumb/111.jpg",
            "likes": 3456,
            "timestamp": "2024-01-25T07:00:00Z"
          },
          {
            "type": "story",
            "storyId": "story-2024-222",
            "author": "@travel_blogger",
            "preview": "Exploring Tokyo!",
            "viewCount": 8900,
            "expiresAt": "2024-01-26T16:00:00Z"
          }
        ],
        "nextCursor": "cursor-xyz-789",
        "hasMore": true
      }
      """;

  private static String safe(String s) {
    return s == null ? "" : s.toLowerCase();
  }
}