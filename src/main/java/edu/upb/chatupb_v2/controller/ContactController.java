package edu.upb.chatupb_v2.controller;

import edu.upb.chatupb_v2.repository.Contact;
import edu.upb.chatupb_v2.repository.ContactDao;
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
        view.onLoad(contacts);
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

    public void eliminar(long id) {
        try {
            contactDao.delete(id);
            onLoad();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
