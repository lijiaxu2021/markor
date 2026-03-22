# Markor 二次开发 - 博客管理功能

## 修改目标
将 Markor 改造为支持 GitHub + Cloudflare Worker 后端的博客管理应用

## 核心修改

### 1. 应用基本信息
- 包名：`net.gsantner.markor` → `com.upxuu.blogadmin`
- 应用名：Markor → Blog Admin
- 版本：2.16.1 → 1.0.0

### 2. 新增功能
- [x] GitHub 文件浏览（替换本地文件浏览）
- [x] Worker API 集成
- [x] 登录界面（API URL + Token）
- [x] 在线编辑和保存
- [x] 图片上传
- [x] 文章元数据支持（Front Matter）

### 3. 修改的文件
- `app/build.gradle` - 修改包名和依赖
- `AndroidManifest.xml` - 修改包名
- `MainActivity.java` - 添加登录逻辑
- `DocumentActivity.java` - 支持在线编辑
- 新增 `WorkerApi.java` - API 客户端

## API 端点
- `GET /api/posts` - 获取文章列表
- `GET /api/post/{filename}` - 获取文章内容
- `PUT /api/post/{filename}` - 保存文章
- `DELETE /api/post/{filename}` - 删除文章
- `POST /api/upload` - 上传图片
- `GET /api/images` - 获取图片列表

## 构建命令
```bash
./gradlew assembleDebug
```
