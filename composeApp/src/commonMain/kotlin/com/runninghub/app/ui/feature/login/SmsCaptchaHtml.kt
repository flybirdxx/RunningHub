package com.runninghub.app.ui.feature.login

/**
 * 构建承载 TAC 验证器的受控 HTML。
 *
 * HTML 按网页端实际抓包到的路径加载 `/tac/js/tac.min.js` 和 `/tac/css/tac.css`，
 * 再由 TAC 内部请求 `/uc/genCaptcha?type=ROTATE` 和 `/uc/checkCaptcha`。这样可以保持
 * 验证码生成、刷新、轨迹校验和 token 回传逻辑与官网一致，避免客户端预取数据后破坏
 * TAC 组件内部状态。
 *
 * 外部脚本通过受控动态加载进入页面，并提供失败、超时和图片解码 watchdog 三类降级文案；
 * 这些文案只说明当前验证码容器不可用，不暴露手机号、短信验证码或服务端返回细节。
 *
 * @param tokenCallbackExpression TAC 校验成功后执行的 JavaScript 表达式。
 * 表达式可以使用局部变量 `token`，其值为服务端返回的短生命周期 `validToken`；
 * 调用方必须只把它用于下一次 `/uc/sendSms` 请求，不得持久化或写入日志。
 * @param closeCallbackExpression TAC 关闭按钮触发时执行的 JavaScript 表达式。
 * 调用方应关闭当前验证码弹窗，但保留登录页手机号等用户输入。
 */
internal fun smsCaptchaHtml(
    tokenCallbackExpression: String,
    closeCallbackExpression: String,
): String =
    """
    <!doctype html>
    <html>
    <head>
      <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
      <link rel="stylesheet" href="/tac/css/tac.css">
      <style>
        html, body { margin: 0; padding: 0; background: #111; color: #fff; overflow: hidden; }
        #captcha { width: 318px; min-height: 318px; margin: 0 auto; position: relative; }
        .status { font: 14px sans-serif; color: #cbd5e1; padding: 96px 24px 24px; text-align: center; }
        .retry { margin-top: 14px; border: 1px solid #a3ff12; color: #a3ff12; background: transparent; border-radius: 8px; padding: 8px 14px; }
      </style>
    </head>
    <body>
      <div id="captcha"><div id="captcha-status" class="status">准备图形验证...</div></div>
      <script>
        (function () {
          var bridgeName = '$CAPTCHA_BRIDGE_NAME';
          function fail(message) {
            document.getElementById('captcha').innerHTML =
              '<div class="status">' + message + '<br><button class="retry" onclick="window.__loadSmsCaptchaScript()">重试</button></div>';
          }
          function notifyNativeToken(token) {
            var callbackToken = token || '';
            // 优先走平台原生 bridge，确保验证成功后立即回到 Compose 状态层关闭弹窗并重试短信发送。
            if (window[bridgeName] && typeof window[bridgeName].onToken === 'function') {
              window[bridgeName].onToken(callbackToken);
              return;
            }
            if (
              window.webkit &&
              window.webkit.messageHandlers &&
              window.webkit.messageHandlers[bridgeName]
            ) {
              window.webkit.messageHandlers[bridgeName].postMessage('token:' + callbackToken);
              return;
            }
            // 仅当平台 bridge 不可用时才退回自定义 scheme，避免导航兜底抢先吞掉稳定回调。
            token = callbackToken;
            $tokenCallbackExpression;
          }
          function notifyNativeClose() {
            if (window[bridgeName] && typeof window[bridgeName].onClose === 'function') {
              window[bridgeName].onClose();
              return;
            }
            if (
              window.webkit &&
              window.webkit.messageHandlers &&
              window.webkit.messageHandlers[bridgeName]
            ) {
              window.webkit.messageHandlers[bridgeName].postMessage('close');
              return;
            }
            $closeCallbackExpression;
          }
          function scheduleRenderWatchdog() {
            window.clearTimeout(window.__captchaWatchdog);
            window.__captchaWatchdog = window.setTimeout(function () {
              var bgImage = document.getElementById('tianai-captcha-slider-bg-img');
              var moveImage = document.getElementById('tianai-captcha-slider-move-img');
              if (!bgImage || !moveImage) {
                fail('图形验证加载失败，请点击重试');
                return;
              }
              if (!bgImage.complete || bgImage.naturalWidth <= 0 || !moveImage.complete || moveImage.naturalWidth <= 0) {
                fail('图形验证图片加载失败，请点击重试');
              }
            }, 8000);
          }
          function initCaptcha() {
            if (!window.TAC) {
              fail('图形验证加载失败，请稍后重试');
              return;
            }
            window.clearTimeout(window.__captchaWatchdog);
            var container = document.getElementById('captcha');
            container.innerHTML = '<div id="captcha-status" class="status">准备图形验证...</div>';
            try {
              var config = new window.CaptchaConfig({
                bindEl: '#captcha',
                requestCaptchaDataUrl: '/uc/genCaptcha?type=ROTATE',
                validCaptchaUrl: '/uc/checkCaptcha',
                isEn: false,
                i18n: {
                  title: '拖动滑块将图片旋转至正确位置',
                  errorText1: '验证码加载失败',
                  errorText2: '验证失败，请重新尝试',
                  successText: '验证成功'
                },
                validSuccess: function (res, captcha, tac) {
                  window.clearTimeout(window.__captchaWatchdog);
                  var token = extractValidToken(res);
                  if (!token) {
                    fail('图形验证已通过，但未返回短信凭证，请点击重试');
                    return;
                  }
                  // 先把 token 交给原生层，由 Compose 状态关闭整个弹窗并重试短信发送；
                  // 不在这里销毁 TAC 窗口，避免桥接失败时只剩空白验证码外壳。
                  notifyNativeToken(token);
                },
                validFail: function (res, captcha, tac) {
                  if (tac && tac.reloadCaptcha) {
                    tac.reloadCaptcha();
                    scheduleRenderWatchdog();
                  } else {
                    fail('验证失败，请点击重试');
                  }
                },
                btnCloseFun: function () {
                  notifyNativeClose();
                },
                btnRefreshFun: function (event, tac) {
                  if (tac && tac.reloadCaptcha) {
                    tac.reloadCaptcha();
                    scheduleRenderWatchdog();
                  } else {
                    initCaptcha();
                  }
                }
              });
              var style = {
                logoUrl: null,
                btnUrl: '/tac/images/huakuai.png',
                moveTrackMaskBgColor: '#0B0E12',
                moveTrackMaskBorderColor: '#0B0E12',
                isEn: false,
                i18n: {
                  title: '拖动滑块将图片旋转至正确位置',
                  errorText1: '验证码加载失败',
                  errorText2: '验证失败，请重新尝试',
                  successText: '验证成功'
                }
              };
              // 按官网组件的调用顺序初始化，让 TAC 自己维护 challenge、轨迹和刷新状态。
              container.innerHTML = '';
              new window.TAC(config, style).init();
              scheduleRenderWatchdog();
            } catch (error) {
              fail('图形验证初始化失败，请点击重试');
            }
          }
          function loadCaptchaScript() {
            window.clearTimeout(window.__captchaScriptTimer);
            window.clearTimeout(window.__captchaWatchdog);
            if (window.TAC) {
              initCaptcha();
              return;
            }
            var oldScript = document.getElementById('runninghub-tac-script');
            if (oldScript && oldScript.parentNode) {
              oldScript.parentNode.removeChild(oldScript);
            }
            document.getElementById('captcha').innerHTML =
              '<div id="captcha-status" class="status">准备图形验证...</div>';
            var script = document.createElement('script');
            script.id = 'runninghub-tac-script';
            script.src = '/tac/js/tac.min.js';
            script.async = true;
            script.onload = function () {
              window.clearTimeout(window.__captchaScriptTimer);
              initCaptcha();
            };
            script.onerror = function () {
              window.clearTimeout(window.__captchaScriptTimer);
              fail('图形验证脚本加载失败，请点击重试');
            };
            window.__captchaScriptTimer = window.setTimeout(function () {
              if (!window.TAC) {
                fail('图形验证脚本加载超时，请点击重试');
              }
            }, 8000);
            document.head.appendChild(script);
          }
          function extractValidToken(res) {
            if (!res) return null;
            if (res.data && res.data.validToken) return res.data.validToken;
            if (typeof res.data === 'string') return res.data;
            if (res.validToken) return res.validToken;
            if (res.data && res.data.token) return res.data.token;
            if (res.token) return res.token;
            return null;
          }
          window.__initSmsCaptcha = initCaptcha;
          window.__loadSmsCaptchaScript = loadCaptchaScript;
          if (document.readyState === 'complete') loadCaptchaScript();
          else window.addEventListener('load', loadCaptchaScript);
        })();
      </script>
    </body>
    </html>
    """.trimIndent()

/**
 * 返回短信图形验证码 Web 容器使用的同源根地址。
 *
 * HTML 内部使用 `/tac/...` 和 `/uc/...` 相对路径，因此 base URL 必须与平台启动层配置的
 * RunningHub Web 环境一致。这样 debug/staging 登录请求不会因为验证码仍访问生产站点而得到
 * 不同环境生成的 `validToken`。
 *
 * @return 带末尾斜杠的 RunningHub Web 根地址。
 */
internal expect fun smsCaptchaBaseUrl(): String

internal const val CAPTCHA_BRIDGE_NAME = "RunningHubSmsCaptcha"
internal const val CAPTCHA_CALLBACK_SCHEME = "runninghub-sms-captcha"
