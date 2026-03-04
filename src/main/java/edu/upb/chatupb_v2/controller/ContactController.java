package edu.upb.chatupb_v2.controller;

import edu.upb.chatupb_v2.model.entities.Contact;
import edu.upb.chatupb_v2.model.repository.ContactDao;
import edu.upb.chatupb_v2.view.ContactInfo;
import edu.upb.chatupb_v2.view.IChatView;

import java.util.ArrayList;
import java.util.List;

public class ContactController {

    private final IChatView view;
    private final ContactDao contactDao;

    public ContactController(IChatView view) {
        this.view = view;
        this.contactDao = new ContactDao();
    }

    public void onLoad() {
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
            contactInfos.add(new ContactInfo(c.getId(), c.getName(), c.getIp()));
        }
        view.onLoad(contactInfos);
    }

    public void guardarContactoSiNoExiste(String idUsuario, String nombre, String ip) {
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
        List<Contact> contacts;
        try {
            contacts = contactDao.findAll();
        } catch (Exception e) {
            e.printStackTrace();
            contacts = new ArrayList<>();
        }
        List<ContactInfo> result = new ArrayList<>();
        for (Contact c : contacts) {
            result.add(new ContactInfo(c.getId(), c.getName(), c.getIp()));
        }
        return result;
    }

    /**
     * Busca el nombre de un contacto por su codigo (ID).
     * Retorna null si no existe.
     */
    public String buscarNombrePorCodigo(String code) {
        try {
            Contact c = contactDao.findByCode(code);
            return c != null ? c.getName() : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void eliminar(long id) {
        try {
            contactDao.delete(id);
            onLoad();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
