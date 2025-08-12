package com.wiseSaying_10;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class App {

  private final BufferedReader br;
  private final List<WiseSaying> quotes;
  private int nextId;

  // 파일 경로 상수
  private final Path baseDir = Paths.get("db", "wiseSaying");
  private final Path lastIdPath = baseDir.resolve("lastId.txt");
  private final Path dataJsonPath = baseDir.resolve("data.json");

  public App() throws IOException {
    this.br = new BufferedReader(new InputStreamReader(System.in));
    this.quotes = new ArrayList<>();
    initStorageAndLoad(); // 9단계: 시작 시 로드
  }

  public void run() throws IOException {
    System.out.println("== 명언 앱 ==");
    while (true) {
      System.out.print("명령) ");
      String command = readLineTrim();

      if (command.equals("종료")) {
        break;
      } else if (command.equals("등록")) {
        handleRegister();
      } else if (command.equals("목록")) {
        handleList();
      } else if (command.startsWith("삭제?id=")) {
        handleDelete(command);
      } else if (command.startsWith("수정?id=")) {
        handleEdit(command);
      } else if (command.equals("빌드")) {  // 10단계
        handleBuild();
      } else {
        System.out.println("지원하지 않는 명령입니다.");
      }
    }
  }

  /* ========== 초기 로딩/저장 관련 (9단계) ========== */

  private void initStorageAndLoad() throws IOException {
    // 폴더 없으면 생성
    Files.createDirectories(baseDir);

    // lastId 읽기
    int lastId = loadLastId();
    this.nextId = lastId + 1;

    // 기존 {id}.json 로드
    loadAllQuotes();
  }

  private int loadLastId() {
    try {
      if (Files.exists(lastIdPath)) {
        String s = Files.readString(lastIdPath).trim();
        if (!s.isEmpty()) return Integer.parseInt(s);
      }
    } catch (Exception ignored) {}
    return 0; // 없으면 0
  }

  private void saveLastId(int lastId) {
    try {
      Files.writeString(lastIdPath, String.valueOf(lastId));
    } catch (IOException e) {
      System.out.println("lastId 저장 중 오류: " + e.getMessage());
    }
  }

  private void loadAllQuotes() {
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(baseDir, "*.json")) {
      for (Path p : stream) {
        // data.json은 스킵 (10단계 결과물)
        if (p.getFileName().toString().equals("data.json")) continue;

        String json = Files.readString(p);
        WiseSaying ws = WiseSaying.fromJson(json);
        // 중복 방지: 같은 id가 이미 있지 않은지 확인 (간단 구현)
        if (findById(ws.getId()) == null) {
          quotes.add(ws);
        }
      }
      // 목록 출력 시 최신순이 필요하면, id 기준 오름/내림 정렬을 여기서 맞춰도 됨 (선택)
      // quotes.sort(Comparator.comparingInt(WiseSaying::getId));
    } catch (IOException e) {
      System.out.println("명언 로딩 중 오류: " + e.getMessage());
    }
  }

  private void saveQuote(WiseSaying q) {
    Path filePath = baseDir.resolve(q.getId() + ".json");
    try {
      Files.writeString(filePath, q.toJson());
    } catch (IOException e) {
      System.out.println(q.getId() + ".json 저장 실패: " + e.getMessage());
    }
  }

  private void deleteQuoteFile(int id) {
    Path filePath = baseDir.resolve(id + ".json");
    try {
      Files.deleteIfExists(filePath);
    } catch (IOException e) {
      System.out.println(id + ".json 삭제 실패: " + e.getMessage());
    }
  }

  /* ========== Command Handlers ========== */

  private void handleRegister() throws IOException {
    String content = prompt("명언 : ");
    String author  = prompt("작가 : ");

    WiseSaying quote = new WiseSaying(nextId++, author, content);
    quotes.add(quote);

    // 파일 반영
    saveQuote(quote);
    saveLastId(quote.getId()); // 마지막 발급 id 갱신

    System.out.println(quote.getId() + "번 명언이 등록되었습니다.");
  }

  private void handleList() {
    System.out.println("번호 / 작가 / 명언");
    System.out.println("---------------------------");

    ListIterator<WiseSaying> it = quotes.listIterator(quotes.size());
    while (it.hasPrevious()) {
      WiseSaying q = it.previous();
      System.out.println(q.getId() + " / " + q.getAuthor() + " / " + q.getContent());
    }
  }

  private void handleDelete(String command) {
    int id = parseId(command);
    if (id < 0) {
      System.out.println("id 파라미터가 올바르지 않습니다. 예) 삭제?id=1");
      return;
    }

    WiseSaying target = findById(id);
    if (target == null) {
      System.out.println(id + "번 명언은 존재하지 않습니다.");
    } else {
      quotes.remove(target);
      // 파일 반영
      deleteQuoteFile(id);
      System.out.println(id + "번 명언이 삭제되었습니다.");
    }
  }

  private void handleEdit(String command) throws IOException {
    int id = parseId(command);
    if (id < 0) {
      System.out.println("id 파라미터가 올바르지 않습니다. 예) 수정?id=2");
      return;
    }

    WiseSaying target = findById(id);
    if (target == null) {
      System.out.println(id + "번 명언은 존재하지 않습니다.");
      return;
    }

    System.out.println("명언(기존) : " + target.getContent());
    String newContent = prompt("명언 : ");

    System.out.println("작가(기존) : " + target.getAuthor());
    String newAuthor = prompt("작가 : ");

    target.setContent(newContent);
    target.setAuthor(newAuthor);

    // 파일 반영(덮어쓰기)
    saveQuote(target);
  }

  // 10단계: data.json 빌드
  private void handleBuild() {
    StringBuilder sb = new StringBuilder();
    sb.append("[\n");

    for (int i = 0; i < quotes.size(); i++) {
      WiseSaying q = quotes.get(i);
      sb.append("  ");
      sb.append(q.toJson().trim().replace("\n", "\n  ")); // 들여쓰기 정리(선택)
      if (i < quotes.size() - 1) sb.append(",");
      sb.append("\n");
    }

    sb.append("]\n");

    try {
      Files.writeString(dataJsonPath, sb.toString());
      System.out.println("data.json 파일의 내용이 갱신되었습니다.");
    } catch (IOException e) {
      System.out.println("data.json 생성 실패: " + e.getMessage());
    }
  }

  /* ========== Helpers ========== */

  private int parseId(String command) {
    try {
      return Integer.parseInt(command.split("id=")[1].trim());
    } catch (Exception e) {
      return -1;
    }
  }

  private WiseSaying findById(int id) {
    for (WiseSaying q : quotes) {
      if (q.getId() == id) return q;
    }
    return null;
  }

  private String readLineTrim() throws IOException {
    return br.readLine().trim();
  }

  private String prompt(String label) throws IOException {
    System.out.print(label);
    return br.readLine();
  }
}

