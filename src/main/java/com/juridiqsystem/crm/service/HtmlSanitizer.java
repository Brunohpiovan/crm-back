package com.juridiqsystem.crm.service;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

/**
 * Sanitiza HTML rico (hoje só o campo "mensagem" de TemplateEmail) antes de persistir. Defesa em
 * profundidade: o editor no frontend (ReactQuill) já sanitiza com DOMPurify usando a mesma
 * allowlist, mas isso é só client-side — nada impede uma chamada direta à API com HTML malicioso
 * no corpo. Sem essa camada, esse HTML seria gravado como veio e reaberto sem filtro por
 * qualquer outro consumidor (ex.: se um dia for renderizado fora do editor).
 */
@Component
public class HtmlSanitizer {

    // Mesma allowlist do frontend (front-crm/src/lib/sanitize.ts, sanitizeRichHtml) — mantém as
    // duas camadas consistentes entre si.
    private static final Safelist TEMPLATE_MENSAGEM_SAFELIST = new Safelist()
            .addTags("p", "br", "strong", "em", "u", "s", "h1", "h2", "h3", "ol", "ul", "li", "a", "img", "span")
            .addAttributes("a", "href", "target", "rel", "class", "style")
            .addAttributes("img", "src", "alt", "class", "style")
            .addAttributes("p", "class", "style")
            .addAttributes("span", "class", "style")
            .addAttributes("h1", "class", "style")
            .addAttributes("h2", "class", "style")
            .addAttributes("h3", "class", "style")
            .addAttributes("li", "class", "style")
            .addAttributes("ol", "class", "style")
            .addAttributes("ul", "class", "style")
            .addProtocols("a", "href", "http", "https", "mailto")
            .addProtocols("img", "src", "http", "https", "data")
            .preserveRelativeLinks(false);

    public String sanitizeTemplateMensagem(String html) {
        if (html == null) {
            return null;
        }
        return Jsoup.clean(html, TEMPLATE_MENSAGEM_SAFELIST);
    }
}
