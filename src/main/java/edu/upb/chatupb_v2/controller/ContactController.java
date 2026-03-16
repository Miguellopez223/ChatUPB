package edu.upb.chatupb_v2.controller;

import edu.upb.chatupb_v2.model.entities.Contact;
import edu.upb.chatupb_v2.model.entities.User;
import edu.upb.chatupb_v2.model.repository.CacheContactDao;
import edu.upb.chatupb_v2.model.repository.ContactDao;
import edu.upb.chatupb_v2.model.repository.IContactDao;
import edu.upb.chatupb_v2.view.ContactInfo;
import edu.upb.chatupb_v2.view.IChatView;

import java.util.ArrayList;
import java.util.List;

public class ContactController {

    private final IChatView view;
    private IContactDao contactDao;
    private User currentUser;

    public ContactController(IChatView view) {
        this.view = view;
        // Inicialmente sin usuario, se setea despues
        // Aplicando el patron Decorador para Cachear el DAO
        this.contactDao = new CacheContactDao(new ContactDao(0));
    }

    public void setUsuario(User user) {
        this.currentUser = user;
        // Aplicando el patron Decorador para Cachear el DAO
        this.contactDao = new CacheContactDao(new ContactDao(user.getId()));
        onLoad(); // Recargar contactos del nuevo usuario
    }

    public void onLoad() {
        if (currentUser == null) return;
        List<Contact> contacts;
        try {
            contacts = contactDao.findAll();
        } catch (Exception e) {
            e.printStackTrace();
            contacts = new ArrayList<>();
        }
        // Convertir entidades del repositorio a DTOs del view
        List<ContactInfo> contactInfos = new ArrayList<>();
        for (Contact c : contacts) {
            contactInfos.add(new ContactInfo(c.getId(), c.getCode(), c.getName(), c.getIp()));
        }
        view.onLoad(contactInfos);
    }

    public void guardarContactoSiNoExiste(String idUsuario, String nombre, String ip) {
        if (currentUser == null) return;
        try {
            if (!contactDao.existByCode(idUsuario)) {
                Contact contacto = Contact.builder()
                        .code(idUsuario)
                        .name(nombre)
                        .ip(ip)
                        .build();
                contactDao.save(contacto);
            } else {
                Contact contacto = contactDao.findByCode(idUsuario);
                contacto.setIp(ip);
                contactDao.update(contacto);
            }
            onLoad();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Retorna la lista de contactos como DTOs para uso del ChatController (ej: Hello).
     */
    public List<ContactInfo> getContactos() {
        if (currentUser == null) return new ArrayList<>();
        List<Contact> contacts;
        try {
            contacts = contactDao.findAll();
        } catch (Exception e) {
            e.printStackTrace();
            contacts = new ArrayList<>();
        }
        List<ContactInfo> result = new ArrayList<>();
        for (Contact c : contacts) {
            result.add(new ContactInfo(c.getId(), c.getCode(), c.getName(), c.getIp()));
        }
        return result;
    }

    /**
     * Busca el nombre de un contacto por su codigo (ID).
     * Retorna null si no existe.
     */
    public String buscarNombrePorCodigo(String code) {
        if (currentUser == null) return null;
        try {
            Contact c = contactDao.findByCode(code);
            return c != null ? c.getName() : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String buscarCodigoPorIp(String ip) {
        if (currentUser == null) return null;
        try {
            Contact c = contactDao.findByIp(ip);
            return c != null ? c.getCode() : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String buscarNombrePorIp(String ip) {
        if (currentUser == null) return null;
        try {
            Contact c = contactDao.findByIp(ip);
            return c != null ? c.getName() : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean existeContactoPorIp(String ip) {
        if (currentUser == null) return false;
        try {
            return contactDao.findByIp(ip) != null;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void eliminar(long id) {
        if (currentUser == null) return;
        try {
            contactDao.delete(id);
            onLoad();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
