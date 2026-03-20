package edu.upb.chatupb_v2.model.repository;

import edu.upb.chatupb_v2.model.entities.Contact;

import java.net.ConnectException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Decorador para IContactDao que añade funcionalidad de caché.
 */
public class CacheContactDao implements IContactDao {

    private final IContactDao decoratedDao;
    private List<Contact> cachedContacts;
    private boolean cacheDirty;

    public CacheContactDao(IContactDao decoratedDao) {
        this.decoratedDao = decoratedDao;
        this.cachedContacts = new ArrayList<>();
        this.cacheDirty = true; // Forzar carga inicial
    }

    @Override
    public void createTableIfNotExists() {
        decoratedDao.createTableIfNotExists();
    }

    @Override
    public void save(Contact contact) throws Exception {
        decoratedDao.save(contact);
        cacheDirty = true; // Invalidar caché
    }

    @Override
    public void update(Contact contact) throws Exception {
        decoratedDao.update(contact);
        cacheDirty = true; // Invalidar caché
    }

    @Override
    public void delete(String id) throws ConnectException, SQLException {
        decoratedDao.delete(id);
        cacheDirty = true; // Invalidar caché
    }

    @Override
    public List<Contact> findAll() throws ConnectException, SQLException {
        if (cacheDirty) {
            cachedContacts = decoratedDao.findAll();
            cacheDirty = false;
        }
        return cachedContacts;
    }

    @Override
    public Contact findByCode(String code) throws ConnectException, SQLException {
        // Buscar primero en caché si está disponible
        if (!cacheDirty) {
            for (Contact contact : cachedContacts) {
                if (contact.getCode().equals(code)) {
                    return contact;
                }
            }
        }
        // Si no está en caché o la caché está sucia, delegar
        return decoratedDao.findByCode(code);
    }

    @Override
    public Contact findByIp(String ip) throws ConnectException, SQLException {
        // Buscar primero en caché si está disponible
        if (!cacheDirty) {
            for (Contact contact : cachedContacts) {
                if (contact.getIp().equals(ip)) {
                    return contact;
                }
            }
        }
        // Si no está en caché o la caché está sucia, delegar
        return decoratedDao.findByIp(ip);
    }

    @Override
    public boolean existByCode(String code) throws ConnectException, SQLException {
        if (!cacheDirty) {
            for (Contact contact : cachedContacts) {
                if (contact.getCode().equals(code)) {
                    return true;
                }
            }
        }
        return decoratedDao.existByCode(code);
    }
}
