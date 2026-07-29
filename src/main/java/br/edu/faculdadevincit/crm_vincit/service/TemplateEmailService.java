package br.edu.faculdadevincit.crm_vincit.service;

import br.edu.faculdadevincit.crm_vincit.model.TemplateEmail;
import br.edu.faculdadevincit.crm_vincit.model.dtos.EmailTemplateRequestDto;
import br.edu.faculdadevincit.crm_vincit.model.dtos.TemplateAllDTO;
import br.edu.faculdadevincit.crm_vincit.model.dtos.TemplateEmailIdDTO;
import br.edu.faculdadevincit.crm_vincit.model.enums.Situacao;
import br.edu.faculdadevincit.crm_vincit.repository.TemplateEmailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TemplateEmailService {

    @Autowired
    private TemplateEmailRepository templateEmailRepository;

    @Autowired
    private S3Service s3Service;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Autowired
    private CloudFrontService cloudFrontService;

    public List<TemplateAllDTO> findAll() {
        return templateEmailRepository.findAllOrdered()
                .stream()
                .map(TemplateAllDTO::new)
                .collect(Collectors.toList());
    }

    public TemplateEmailIdDTO findById(Long id) {
        TemplateEmail template = templateEmailRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template não encontrada"));

        TemplateEmailIdDTO dto = new TemplateEmailIdDTO(template);

        List<String> anexos = dto.getUrlAnexo();

        if (anexos != null && !anexos.isEmpty()) {
            List<String> urlsAssinadas = anexos.stream()
                    .map(url -> cloudFrontService.generateSignedUrl(url, Duration.ofMinutes(60)))
                    .collect(Collectors.toList());
            dto.setUrlAnexo(urlsAssinadas);
        }

        return dto;
    }


    public void create(EmailTemplateRequestDto dto) {
        TemplateEmail templateEmail = new TemplateEmail();
        templateEmail.setNome(dto.getNome());
        templateEmail.setAssunto(dto.getAssunto());
        templateEmail.setMensagem(dto.getMensagem());
        try {
            templateEmail.setSituacao(Situacao.valueOf(dto.getSituacao().toUpperCase().replace(" ", "_")));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Valor inválido para situação: " + dto.getSituacao());
        }


        if (dto.getAnexos() != null && !dto.getAnexos().isEmpty()) {
            List<String> urlAnexos = new ArrayList<>();
            for (MultipartFile file : dto.getAnexos()) {
                String key = "anexos/" + file.getOriginalFilename();
                String fileUrl = s3Service.uploadFile(file, key);
                urlAnexos.add(fileUrl);
            }
            templateEmail.setUrlAnexo(urlAnexos);
        }
        templateEmail.setCriadoEm(LocalDateTime.now());
        templateEmailRepository.save(templateEmail);
    }

    public void update(Long id, EmailTemplateRequestDto dto) {
        TemplateEmail templateEmailBanco = templateEmailRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template não encontrado"));

        templateEmailBanco.setNome(dto.getNome());
        templateEmailBanco.setAssunto(dto.getAssunto());
        templateEmailBanco.setMensagem(dto.getMensagem());

        try {
            templateEmailBanco.setSituacao(Situacao.valueOf(dto.getSituacao().toUpperCase().replace(" ", "_")));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Valor inválido para situação: " + dto.getSituacao());
        }

        List<String> urlAnexosExistentes = templateEmailBanco.getUrlAnexo();
        if (urlAnexosExistentes == null) {
            urlAnexosExistentes = new ArrayList<>();
        }

        List<String> urlAnexosNovos = dto.getUrlAnexo() != null ? dto.getUrlAnexo() : new ArrayList<>();

        List<String> urlsParaRemover = new ArrayList<>(urlAnexosExistentes);
        urlsParaRemover.removeAll(urlAnexosNovos);

        for (String url : urlsParaRemover) {
            String key = url.replace(baseUrl + "/", "");
            s3Service.deleteFile(key);
            urlAnexosExistentes.remove(url);
        }

        if (dto.getAnexos() != null && !dto.getAnexos().isEmpty()) {
            for (MultipartFile file : dto.getAnexos()) {
                String key = "anexos/" + file.getOriginalFilename();
                String fileUrl = s3Service.uploadFile(file, key);
                urlAnexosExistentes.add(fileUrl);
            }
        }

        templateEmailBanco.setUrlAnexo(urlAnexosExistentes);
        templateEmailBanco.setAtualizadoEm(LocalDateTime.now());
        templateEmailRepository.save(templateEmailBanco);
    }
    @Value("${aws.s3.base-url}")
    private String baseUrl;
    public void delete(Long id){
        TemplateEmail templateEmailBanco = templateEmailRepository.findById(id).orElseThrow(()->new RuntimeException("Template nao encontrada"));
        templateEmailRepository.delete(templateEmailBanco);
    }



}
