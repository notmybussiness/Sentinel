# Backend Scripts - Performance Testing

> **Last Updated**: 2025-11-25  
> **Environment**: 3-Tier Physical Setup (Windows WAS + Linux DB + Mac Load Generator)

---

## 📁 Directory Structure

```
scripts/
├── README.md                    # This file
├── .gitattributes              # Git LFS configuration
├── common/                      # Shared utilities
│   ├── python/
│   │   ├── export_metrics.py   # Prometheus metrics extraction
│   │   └── analyze_cache_metrics.py
│   └── k6/
│       └── utils.js            # k6 common functions
├── templates/                   # Templates for new phases
│   ├── README.md.template
│   └── REPORT.md.template
├── phase1_cache_experiments/    # Phase 1
├── phase2_external_api_optimization/
├── phase3_db_optimization/
├── phase3b_cache_validation/
└── metrics_export/              # Exported Prometheus data
```

---

## 🌍 Infrastructure

| Component | Location | IP:Port | Specs |
|-----------|----------|---------|-------|
| **WAS** | Windows PC | 192.168.0.58:8080 | Intel i5-12400F, 16GB RAM |
| **DB + Monitoring** | Linux Server | 192.168.0.5:5432 | AMD Ryzen 5 3400G, 12GB RAM |
| **Load Generator** | MacBook | - | Apple M1, 8GB RAM |

**File Sharing**: Naver Cloud  
**Monitoring**: Grafana (http://192.168.0.5:3001), Prometheus (http://192.168.0.5:9090)

---

## � Standard Workflow

### 1. Windows에서 준비
```bash
# k6 스크립트 작성
cd phaseN_name/tests/

# Naver Cloud에 업로드
# - tests/ 폴더
# - common/k6/utils.js
# - (필요시) 토큰 파일
```

### 2. Mac에서 실행
```bash
# Naver Cloud에서 다운로드
cd ~/k6_tests/phaseN_name

# k6 실행
k6 run tests/expN.js --out json=results/expN/result.json

# 결과 업로드 (Naver Cloud)
```

### 3. Windows에서 분석
```bash
# 결과 다운로드
# phaseN_name/results/expN/ 에 저장

# Prometheus 메트릭 추출
python common\python\export_metrics.py \
  --prometheus-url http://192.168.0.5:9090 \
  --start "2025-XX-XXTXX:XX:XXZ" \
  --end "2025-XX-XXTXX:XX:XXZ" \
  --output phaseN_name\results\expN

# 분석
cd phaseN_name\analysis
python analyze.py
```

---

## 📦 Git Strategy

**Git LFS 사용** (대용량 결과 파일 관리):
```bash
# 이미 설정됨
git lfs track "phase*/results/**/*.csv"
git lfs track "phase*/results/**/*.json"

# 커밋
git add phase1_cache_experiments/results/exp1/
git commit -m "Phase 1 Exp 1 results"
git push
```

**장점**:
- ✅ 완전한 재현성 (코드 + 데이터)
- ✅ 성능 변화 추적
- ✅ 포트폴리오 증거

---

## 📊 Completed Phases

| Phase | Status | Key Result |
|-------|--------|------------|
| Phase 1: Cache Experiments | ✅ Complete | [Link to REPORT](phase1_cache_experiments/REPORT.md) |
| Phase 2: External API Optimization | ✅ Complete | 210x improvement with service-layer cache |
| Phase 3: DB Optimization | ✅ Complete | N+1 solved with EntityGraph |
| Phase 3b: Cache Validation | ✅ Complete | Cache hit rate >80% |

---

## 🚀 Starting a New Phase

```bash
# 1. Template 복사
cp -r templates phaseN_new_name

# 2. README 작성
cd phaseN_new_name
# README.md.template → README.md 수정

# 3. tests/ 폴더에 k6 스크립트 작성
cd tests
# exp1_baseline.js 작성

# 4. Phase 실행 (위의 Standard Workflow 참고)
```

---

## 🔧 Common Utilities

### export_metrics.py
Prometheus에서 메트릭 추출:
```bash
python common\python\export_metrics.py \
  --prometheus-url http://192.168.0.5:9090 \
  --last 1h \
  --single-file
```

### k6 utils.js
k6 스크립트에서 공통 함수 사용:
```javascript
import { getRandomToken } from '../../common/k6/utils.js';
```

---

## 📝 Notes

- **k6 설치 (Mac)**: `brew install k6`
- **Python 3.10+** 권장 (Windows)
- **Git LFS**: `git lfs install` (한 번만)

---

**Maintained by**: Sentinel Squad 🛡️
