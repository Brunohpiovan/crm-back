package br.edu.faculdadevincit.crm_vincit.controller;
import br.edu.faculdadevincit.crm_vincit.model.MensagemRequest;
import br.edu.faculdadevincit.crm_vincit.service.WhatsAppService;
import com.twilio.rest.api.v2010.account.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/whatsapp")
public class WhatsAppController {

    @Autowired
    private WhatsAppService whatsAppService;

    @PostMapping("/send")
    public Message sendMessage(@RequestBody MensagemRequest mensagemRequest) {
        return whatsAppService.sendWhatsAppMessage(mensagemRequest);
    }

    @PostMapping("/webhook")
    public void receiveWhatsAppMessage(@RequestParam Map<String, String> params) {
        whatsAppService.receiveRequest(params);
    }
}



