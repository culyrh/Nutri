#!/usr/bin/env python3
"""
NutriUniv 쿠팡 수동 매핑 CLI.

서버 (로컬에서 ./gradlew bootRun 으로 띄운 상태) 에 HTTP 호출하면서
터미널에서 사용자 입력을 받아 한 건씩 매핑한다.

사용법:
  python3 manual_map.py                          # 인터랙티브로 시작 ID 입력 받음
  python3 manual_map.py --start-from 6800        # 6800 부터 시작
  python3 manual_map.py --status FAILED LINKED   # 여러 상태 동시
  python3 manual_map.py --status ALL             # 전체
  python3 manual_map.py --to-id 10000            # 10000 까지만
  python3 manual_map.py --server http://localhost:8080
"""

import argparse
import sys
import textwrap
from typing import Any, Dict, List, Optional

try:
    import requests
except ImportError:
    print("✗ requests 라이브러리가 필요합니다. 다음 명령으로 설치하세요:")
    print("    pip3 install requests")
    sys.exit(1)


# ── ANSI 색상 (터미널 가독성) ──────────────────────────────────────────────────
RESET   = "\033[0m"
BOLD    = "\033[1m"
DIM     = "\033[2m"
RED     = "\033[31m"
GREEN   = "\033[32m"
YELLOW  = "\033[33m"
BLUE    = "\033[34m"
MAGENTA = "\033[35m"
CYAN    = "\033[36m"


def c(text: str, color: str) -> str:
    return f"{color}{text}{RESET}"


# ── HTTP 호출 ─────────────────────────────────────────────────────────────────
class Api:
    def __init__(self, base_url: str):
        self.base_url = base_url.rstrip("/")
        self.session = requests.Session()

    def queue(self, statuses: List[str], from_id: int) -> Dict[str, Any]:
        params = [("fromId", from_id)] + [("status", s) for s in statuses]
        r = self.session.get(f"{self.base_url}/admin/coupang/manual/queue",
                             params=params, timeout=30)
        r.raise_for_status()
        return r.json()["data"]

    def search(self, product_id: int, keyword: Optional[str] = None) -> Dict[str, Any]:
        params = {"productId": product_id}
        if keyword:
            params["keyword"] = keyword
        # 쿠팡 호출이 throttle 로 최대 ~3초 + 응답 대기. 여유 있게 60초 타임아웃.
        r = self.session.get(f"{self.base_url}/admin/coupang/manual",
                             params=params, timeout=60)
        r.raise_for_status()
        return r.json()["data"]

    def action(self, product_id: int, body: Dict[str, Any]) -> Dict[str, Any]:
        r = self.session.post(f"{self.base_url}/admin/coupang/manual/{product_id}",
                              json=body, timeout=30)
        r.raise_for_status()
        return r.json()["data"]


# ── 화면 렌더링 ───────────────────────────────────────────────────────────────
def render_product_header(idx: int, total: int, product_id: int, product_name: str):
    print()
    print(c("─" * 78, DIM))
    print(f"{c(f'[{idx}/{total}]', BOLD)}  "
          f"productId={c(str(product_id), CYAN)}  "
          f"name={c(product_name, BOLD)}")


def render_current_mapping(cm: Optional[Dict[str, Any]]):
    if not cm:
        print(c("  현재 매핑: (없음)", DIM))
        return
    status     = cm.get("linkStatus", "?")
    coupang    = cm.get("coupangProductName") or "(매핑 데이터 없음)"
    color      = GREEN if status == "LINKED" else (YELLOW if status in ("FAILED", "UNLINKED") else DIM)
    print(f"  현재 상태: {c(status, color)}")
    if status == "LINKED":
        print(f"  매핑된 쿠팡 상품: {c(coupang, MAGENTA)}")


def render_candidates(candidates: List[Dict[str, Any]], keyword: str):
    print(c(f"\n  검색 키워드: \"{keyword}\"", DIM))
    if not candidates:
        print(c("  ⚠ 검색 결과 0건", YELLOW))
        return
    for c_ in candidates:
        idx     = c_["index"]
        name    = c_["productName"] or "(이름 없음)"
        price   = c_.get("productPrice")
        rocket  = c_.get("isRocket")
        free    = c_.get("isFreeShipping")
        price_s = f"₩{price:,}" if price else ""
        tags    = []
        if rocket: tags.append(c("[로켓]", BLUE))
        if free:   tags.append(c("[무배]", GREEN))
        tag_s   = " ".join(tags)
        # 긴 이름은 한 줄로 잘려도 OK (사용자가 보고 판단)
        print(f"  {c(f'{idx:>2}.', BOLD)} {name}   {c(price_s, DIM)} {tag_s}")


def print_help():
    print(c(textwrap.dedent("""
        ─── 키 ───────────────────────────────────────────
          숫자        : 그 번호의 후보로 매핑 (덮어쓰기 포함)
          엔터        : 변경 없이 다음 상품
          s           : 매칭 포기 (영구 SKIPPED 처리)
          r <키워드>   : 키워드 바꿔서 같은 상품 재검색
                        예) r 비타민C
          h           : 이 도움말 다시 보기
          q           : 종료 (마지막 처리 productId 출력)
        ─────────────────────────────────────────────────
    """), DIM))


# ── 메인 루프 ─────────────────────────────────────────────────────────────────
def main():
    parser = argparse.ArgumentParser(
        description="NutriUniv 쿠팡 수동 매핑 CLI",
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument("--server",     default="http://localhost:8080",
                        help="서버 URL (기본: http://localhost:8080)")
    parser.add_argument("--status",     nargs="+", default=None,
                        help="대상 상태 (LINKED/UNLINKED/FAILED/SKIPPED/ALL). 미지정 시 FAILED.")
    parser.add_argument("--start-from", type=int, default=None,
                        help="이 productId 이상부터 시작 (재개용)")
    parser.add_argument("--to-id",      type=int, default=None,
                        help="이 productId 까지만 (포함). 팀원 분담용.")
    args = parser.parse_args()

    api = Api(args.server)

    print(c("━" * 78, BOLD))
    print(c("  NutriUniv 쿠팡 수동 매핑 CLI", BOLD))
    print(c("━" * 78, BOLD))
    print(f"  서버: {args.server}")

    # 시작 ID 결정 (인자 우선, 없으면 인터랙티브)
    if args.start_from is None:
        raw = input("시작 productId 입력 (엔터 = 처음부터): ").strip()
        start_from = int(raw) if raw else 0
    else:
        start_from = args.start_from
    print(f"  시작 ID: {start_from}")

    # 상태 필터
    statuses = args.status if args.status else ["FAILED"]
    print(f"  대상 상태: {', '.join(statuses)}")
    if args.to_id is not None:
        print(f"  종료 ID: {args.to_id}")

    print_help()

    # 작업 큐 로드
    try:
        queue_data = api.queue(statuses, start_from)
    except requests.HTTPError as e:
        print(c(f"✗ 큐 조회 실패: {e.response.status_code} {e.response.text}", RED))
        sys.exit(1)
    except Exception as e:
        print(c(f"✗ 서버 연결 실패: {e}", RED))
        print(c("  서버가 떠 있는지 확인하세요: curl http://localhost:8080/admin/coupang/manual/queue", DIM))
        sys.exit(1)

    all_ids: List[int] = queue_data["productIds"]
    if args.to_id is not None:
        all_ids = [pid for pid in all_ids if pid <= args.to_id]

    if not all_ids:
        print(c("\n작업 대상이 없습니다.", YELLOW))
        sys.exit(0)

    print(f"\n작업 대상 {c(str(len(all_ids)), BOLD)} 건. 시작합니다.\n")

    # 통계
    stat_select = 0
    stat_skip   = 0
    stat_pass   = 0
    last_id: Optional[int] = None

    i = 0
    while i < len(all_ids):
        pid = all_ids[i]
        keyword_override: Optional[str] = None

        # 한 productId 에 대한 검색 (재검색 루프)
        while True:
            try:
                data = api.search(pid, keyword_override)
            except requests.HTTPError as e:
                print(c(f"✗ 검색 실패 (productId={pid}): {e.response.status_code} {e.response.text}", RED))
                i += 1
                break
            except Exception as e:
                print(c(f"✗ 검색 중 오류 (productId={pid}): {e}", RED))
                i += 1
                break

            product    = data["product"]
            cm         = data.get("currentMapping")
            candidates = data.get("candidates") or []
            keyword    = data.get("searchKeyword") or product["name"]
            landing    = data.get("landingUrl")

            render_product_header(i + 1, len(all_ids), product["id"], product["name"])
            render_current_mapping(cm)
            render_candidates(candidates, keyword)

            choice = input(c("\n선택 [번호/엔터/s/r/h/q]: ", BOLD)).strip()

            # ── 엔터 = 다음 상품, 변경 없음 ──
            if choice == "":
                stat_pass += 1
                last_id = pid
                i += 1
                break

            # ── q: 종료 ──
            if choice.lower() == "q":
                print_summary(stat_select, stat_skip, stat_pass, last_id)
                return

            # ── h: 도움말 ──
            if choice.lower() == "h":
                print_help()
                continue  # 같은 상품 다시 표시

            # ── s: SKIP ──
            if choice.lower() == "s":
                try:
                    res = api.action(pid, {
                        "action": "SKIP",
                        "searchKeyword": keyword,
                    })
                    print(c(f"  ✓ [productId={pid}] {res.get('message', 'SKIPPED')}", YELLOW))
                    stat_skip += 1
                    last_id = pid
                except Exception as e:
                    print(c(f"  ✗ SKIP 실패: {e}", RED))
                i += 1
                break

            # ── r <키워드>: 키워드 변경 후 재검색 (같은 상품) ──
            if choice.lower().startswith("r"):
                new_kw = choice[1:].strip()
                if not new_kw:
                    print(c("  ⚠ 'r' 뒤에 키워드를 입력하세요. 예: r 비타민C", YELLOW))
                    continue
                keyword_override = new_kw
                continue  # 같은 상품 다시 검색

            # ── 숫자: 후보 선택 ──
            if choice.isdigit():
                num = int(choice)
                match = next((c_ for c_ in candidates if c_["index"] == num), None)
                if match is None:
                    print(c(f"  ⚠ {num}번 후보가 없습니다. (1~{len(candidates)})", YELLOW))
                    continue
                try:
                    res = api.action(pid, {
                        "action":        "SELECT",
                        "candidate":     match,
                        "searchKeyword": keyword,
                        "landingUrl":    landing,
                    })
                    print(c(f"  ✓ [productId={pid}] 매핑 → {res.get('coupangProductName', '')}", GREEN))
                    stat_select += 1
                    last_id = pid
                except Exception as e:
                    print(c(f"  ✗ 매핑 실패: {e}", RED))
                i += 1
                break

            # ── 그 외 ──
            print(c(f"  ⚠ 알 수 없는 입력: '{choice}'. h 입력 시 도움말.", YELLOW))

    # 큐 끝
    print(c("\n전체 큐 완료!", GREEN))
    print_summary(stat_select, stat_skip, stat_pass, last_id)


def print_summary(select: int, skip: int, passed: int, last_id: Optional[int]):
    print()
    print(c("━" * 78, BOLD))
    print(c("  종료", BOLD))
    print(c("━" * 78, BOLD))
    print(f"  매핑(SELECT): {c(str(select), GREEN)}")
    print(f"  스킵(SKIP):   {c(str(skip),   YELLOW)}")
    print(f"  넘김(엔터):    {c(str(passed), DIM)}")
    if last_id is not None:
        print(f"\n  마지막 처리 productId: {c(str(last_id), CYAN)}")
        print(f"  다음 실행 시: {c(f'python3 manual_map.py --start-from {last_id + 1}', DIM)}")
    print()


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print(c("\n\n인터럽트로 종료됨.", YELLOW))
        sys.exit(130)
