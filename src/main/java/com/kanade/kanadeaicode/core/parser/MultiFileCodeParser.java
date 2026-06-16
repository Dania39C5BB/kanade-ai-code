package com.kanade.kanadeaicode.core.parser;

import com.kanade.kanadeaicode.ai.model.MultiFileCodeResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 多文件代码解析器（HTML + CSS + JS）
 *
 * @author kanade
 */
public class MultiFileCodeParser implements CodeParser<MultiFileCodeResult> {

    // 使用 \\s* 而非 \\s*\\n，避免 AI 输出格式细微偏差（如换行缺失）导致匹配失败
    private static final Pattern HTML_CODE_PATTERN = Pattern.compile("```html\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    private static final Pattern CSS_CODE_PATTERN = Pattern.compile("```css\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    private static final Pattern JS_CODE_PATTERN = Pattern.compile("```(?:js|javascript)\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    @Override
    public MultiFileCodeResult parseCode(String codeContent) {
        MultiFileCodeResult result = new MultiFileCodeResult();
        // 提取各类代码
        String htmlCode = extractCodeByPattern(codeContent, HTML_CODE_PATTERN);
        String cssCode = extractCodeByPattern(codeContent, CSS_CODE_PATTERN);
        String jsCode = extractCodeByPattern(codeContent, JS_CODE_PATTERN);
        // 设置HTML代码
        if (htmlCode != null && !htmlCode.trim().isEmpty()) {
            result.setHtmlCode(htmlCode.trim());
        }
        // 设置CSS代码
        if (cssCode != null && !cssCode.trim().isEmpty()) {
            result.setCssCode(cssCode.trim());
        }
        // 设置JS代码
        if (jsCode != null && !jsCode.trim().isEmpty()) {
            result.setJsCode(jsCode.trim());
        }
        return result;
    }

    /**
     * 根据正则提取代码，合并所有匹配块（防止 AI 输出多个同语言代码块时只取第一个）
     *
     * @param content 原始内容
     * @param pattern 正则模式
     * @return 提取的代码（多个块合并，用换行分隔）
     */
    private String extractCodeByPattern(String content, Pattern pattern) {
        Matcher matcher = pattern.matcher(content);
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

