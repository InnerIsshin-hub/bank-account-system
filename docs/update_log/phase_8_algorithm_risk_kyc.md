# 第 8 阶段：算法端风险检测与 KYC OCR/人脸识别

- 根目录新增 `algorithm/` Python 算法服务，可独立启动在 `http://localhost:8090`。
- 新增 `POST /api/aml/evaluate`：基于交易图谱和可解释规则检测资金风险，覆盖临界拆分、多笔小额化、快速过桥、多入多出、资金环路等洗钱风险模式，输出账户/交易风险分、等级、动作和原因。
- 新增 `POST /api/kyc/ocr-face`：基于 Tesseract OCR、OpenCV 和图像质量评估识别证件字段、人脸区域、清晰度/亮度/反光，并支持多帧差异活体评分。
- 算法端不进行深度学习训练，满足可运行、可解释、可演示要求。
- 新增 `algorithm/Dockerfile`、`requirements.txt`、样例交易数据和 pytest 用例。
- `docker-compose.yml` 新增 `bank-algorithm` 服务，端口 `8090`。

验证：

- 当前 Ubuntu 容器已安装 `tesseract-ocr` 和 `tesseract-ocr-chi-sim`。
- `algorithm/` 执行 `pytest -q` 通过，3 个算法测试全部通过。
- 算法服务 `/health` 返回 `UP`。
- AML 烟测识别 4 笔风险交易，高风险账户为 `A100`。
- KYC OCR/人脸烟测成功识别身份证号 `110101199001010011`，检测到 1 张人脸区域，返回可解释决策。
