package fullclanker.alchemist.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.io.Serializable;

/** One item in one user's collection; the (user, item) pair is the primary key, so it is unique. */
@Entity
@Table(name = "user_item")
@IdClass(UserItem.Key.class)
public class UserItem {

	@Id
	@ManyToOne(optional = false)
	@JoinColumn(name = "user_id", nullable = false, options = "REFERENCES user (id)")
	private User user;

	@Id
	@ManyToOne(optional = false)
	@JoinColumn(name = "item_id", nullable = false, options = "REFERENCES item (id)")
	private Item item;

	protected UserItem() {
	}

	public UserItem(User user, Item item) {
		this.user = user;
		this.item = item;
	}

	public User getUser() {
		return user;
	}

	public Item getItem() {
		return item;
	}

	public record Key(Long user, Long item) implements Serializable {
	}

}
