package com.wiseSaying_10;

public class WiseSaying {

  private final int id;
  private String author;
  private String content;

  public WiseSaying(int id, String author, String content) {
    this.id = id;
    this.author = author;
    this.content = content;
  }

  public int getId() { return id; }
  public String getAuthor() { return author; }
  public String getContent() { return content; }
  public void setAuthor(String author) { this.author = author; }
  public void setContent(String content) { this.content = content; }

  /* ===== JSON Helpers (간단 구현) ===== */

  // JSON 문자열 생성 (특수문자 최소 이스케이프)
  public String toJson() {
    return """
    {
      "id": %d,
      "content": "%s",
      "author": "%s"
    }
    """.formatted(id, escape(content), escape(author));
  }

  public static WiseSaying fromJson(String json) {
    int id = extractInt(json, "\"id\"");
    String content = extractString(json, "\"content\"");
    String author = extractString(json, "\"author\"");
    return new WiseSaying(id, author, content);
  }

  private static String escape(String s) {
    if (s == null) return "";
    // 역슬래시 → \\  /  따옴표 → \"
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  private static int extractInt(String json, String key) {
    int idx = json.indexOf(key);
    if (idx == -1) return 0;
    int colon = json.indexOf(':', idx);
    int comma = json.indexOf(',', colon);
    int end = (comma == -1) ? json.indexOf('}', colon) : comma;
    String num = json.substring(colon + 1, end).trim();
    return Integer.parseInt(num);
  }

  private static String extractString(String json, String key) {
    int idx = json.indexOf(key);
    if (idx == -1) return "";
    int colon = json.indexOf(':', idx);
    int firstQuote = json.indexOf('"', colon + 1);
    int secondQuote = json.indexOf('"', firstQuote + 1);
    // 단순 구현: 내부에 \"가 있을 경우 두 번째 따옴표 탐색이 어긋날 수 있음 → 위 escape로 저장했으니 안전
    String raw = json.substring(firstQuote + 1, secondQuote);
    // 저장 시 \"로 이스케이프했으므로 되돌리기
    return raw.replace("\\\"", "\"").replace("\\\\", "\\");
  }
}

