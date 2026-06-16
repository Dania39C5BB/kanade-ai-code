package com.kanade.kanadeaicode.core.parser;

import com.kanade.kanadeaicode.ai.model.HtmlCodeResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTML 单文件代码解析器
 *
 * @author kanade
 */
public class HtmlCodeParser implements CodeParser<HtmlCodeResult> {

    // 使用 \\s* 而非 \\s*\\n，避免 AI 输出格式细微偏差（如换行缺失）导致匹配失败
    private static final Pattern HTML_CODE_PATTERN = Pattern.compile("```html\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    @Override
    public HtmlCodeResult parseCode(String codeContent) {
        HtmlCodeResult result = new HtmlCodeResult();
        // 提取 HTML 代码
        String htmlCode = extractHtmlCode(codeContent);
        if (htmlCode != null && !htmlCode.trim().isEmpty()) {
            result.setHtmlCode(htmlCode.trim());
        } else {
            // 如果没有找到代码块，将整个内容作为HTML
            result.setHtmlCode(codeContent.trim());
        }
        return result;
    }

    /**
     * 提取HTML代码内容，合并所有匹配块（防止 AI 输出多个 HTML 代码块时只取第一个）
     */
    private String extractHtmlCode(String content) {
        Matcher matcher = HTML_CODE_PATTERN.matcher(content);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String code = matcher.group(1);
            if (code != null && !code.trim().isEmpty()) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(code.trim());
            }
        }
        return sb.length() > 0 ? sb.toString() : null;
    }
}

