package com.juridiqsystem.crm.service.auth;


import com.juridiqsystem.crm.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService implements UserDetailsService {
    @Autowired
    UsuarioRepository usuarioRepository;

    /**
     * "username" aqui é o composto "codigoEmpresa|login" montado por AuthenticationService —
     * login sozinho não basta mais pra achar um Usuario único, já que passou a ser único por
     * Empresa (duas empresas podem ter funcionário com o mesmo e-mail).
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        int separatorIndex = username.indexOf('|');
        if (separatorIndex < 0) {
            throw new IllegalArgumentException("Usuário não encontrado.");
        }
        String codigoEmpresa = username.substring(0, separatorIndex);
        String login = username.substring(separatorIndex + 1);
        return usuarioRepository.findByEmpresaCodigoAndLogin(codigoEmpresa, login)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
    }
}
