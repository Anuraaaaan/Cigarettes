package com.anuraaaan.cigarettes.listener;

import com.anuraaaan.cigarettes.Cigarettes;
import com.anuraaaan.cigarettes.cigarette.CigaretteRegistry;
import com.anuraaaan.cigarettes.cigarette.ConfiguredCigarette;
import com.anuraaaan.cigarettes.utils.CigaretteSoundPlayer;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

// ивент по курению сигареты
public class SmokingListener implements Listener {

    private static final Cigarettes plugin = Cigarettes.getPlugin();
    // ключи сигареты из pdc
    private static final NamespacedKey KEY_ID = new NamespacedKey(plugin, "cigarette_id");
    private static final NamespacedKey KEY_TIME = new NamespacedKey(plugin, "last_use");
    // создаёт экземпляр сигареты
    private static ConfiguredCigarette litCigarette = null;


    public SmokingListener() {
        startTimer();
    }

    @EventHandler
    public void smoking(PlayerItemConsumeEvent e) {
        // получает игрока
        Player player = e.getPlayer();

        // берётся предмет из руки и проверяется на сходство с сигаретой
        ItemStack item = player.getInventory().getItemInMainHand();
        String id = CigaretteRegistry.getId(item);
        if (id == null) return;

        // сохраняет объект класса сигареты по id
        litCigarette = CigaretteRegistry.getLitCigarettes().get(id);

        // берётся конфиг для проверки на пустоту и изменения pdc времени (в будущем и кол-ва затяжек)
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        // получение pdc и проверка на наличие ключа id
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // извлекает временную метку создания/последнего обновления предмета
        // если данных нет (предмет новый), использует 0 как значение по умолчанию
        long lastUse = pdc.getOrDefault(KEY_TIME, PersistentDataType.LONG, 0L); // если данных нет, то lastUse = 0

        // искусственно "старит" предмет, сдвигая момент его создания назад
        // это сокращает оставшееся время жизни (LIFETIME) на величину штрафа
        lastUse -= litCigarette.getBurnTimePerPuff();

        // перезаписывает обновленную метку в метаданные предмета
        pdc.set(KEY_TIME, PersistentDataType.LONG, lastUse);

        // сохраняет мету предмета
        item.setItemMeta(meta);
        // получает локацию игрока
        Location location = player.getLocation();
        Location eyeLocation = player.getEyeLocation();

        // спавнит партиклы
        litCigarette.spawnParticles(eyeLocation);

        // проигрывает музыку
        CigaretteSoundPlayer.playSound(litCigarette, location, "burning");
        // выдаёт эффекты
        for (PotionEffect effect : litCigarette.getEffects()) {
            player.addPotionEffect(effect);
        }

        player.sendMessage("Ты использовал сигарету");

        // возвращает предмет, чтобы он не удалялся после использования
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.getInventory().setItemInMainHand(item);
        });
    }
    // выполняет действие каждую секунду
    private void startTimer() {
        new BukkitRunnable() {

            @Override
            public void run() {
                // смотрит инвентарь всех онлайн игроков
                for (Player player : Bukkit.getOnlinePlayers()) {
                    for (ItemStack item : player.getInventory().getContents()) {
                        // проверяет, есть ли предмет
                        if (item == null) continue;

                        // берёт айди предмета и проверяет его наличие
                        String id = CigaretteRegistry.getId(item);
                        if (id == null) continue;

                        // сохраняет объект класса сигареты по id
                        litCigarette = CigaretteRegistry.getLitCigarettes().get(id);

                        // проверяет наличие меты
                        ItemMeta meta = item.getItemMeta();
                        if (meta == null) continue;

                        // берёт pdc предмета
                        PersistentDataContainer pdc = meta.getPersistentDataContainer();

                        // проверяет наличие айди и времени
                        if (!pdc.has(KEY_ID, PersistentDataType.STRING)) continue;
                        if (!pdc.has(KEY_TIME, PersistentDataType.LONG)) continue;

                        // извлекает временную метку создания/последнего обновления предмета
                        // если данных нет (предмет новый), использует 0 как значение по умолчанию
                        long lastUse = pdc.getOrDefault(KEY_TIME, PersistentDataType.LONG, 0L);

                        // берёт время горения подходящее предмету
                        long burnDuration = litCigarette.getBurnDuration();

                        // рассчитывается остаток времени
                        long remainingMS = burnDuration - (System.currentTimeMillis() - lastUse);

                        /* проверяет сколько времени осталось. использовалось для проверки
                        if (remainingMS > 0) {
                            long remainingSec = remainingMS / 1000;
                            player.sendMessage("Осталось: " + remainingSec);
                        } */

                        // получает процент остатка предмета
                        int percent = (int) ((remainingMS * 100) / burnDuration);

                        // ищет минимальный порог (ключ), который больше или равен текущему значению процента
                        Integer threshold = litCigarette.getItemModels().ceilingKey(percent);
                        // если подходящий порог найден в мапе моделей
                        if (threshold != null) {
                            // получает строковый идентификатор модели по найденному порогу,
                            // создаёт из него NamespacedKey и устанавливает в метаданные предмета
                            meta.setItemModel(NamespacedKey.fromString(litCigarette.getItemModels().get(threshold)));
                        }
                        // применяет мету предмету
                        item.setItemMeta(meta);

                        // если время истекло, то удаляется сигарета из инвентаря
                        if (System.currentTimeMillis() - lastUse >= burnDuration) {
                            player.getInventory().remove(item);

                            // получает координаты и проигрывает звук
                            Location location = player.getLocation();
                            Location eye = player.getEyeLocation();
                            CigaretteSoundPlayer.playSound(litCigarette, location, "extinguishing");
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }
}
