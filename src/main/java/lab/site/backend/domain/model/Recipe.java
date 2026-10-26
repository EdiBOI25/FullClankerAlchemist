package lab.site.backend.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "recipe")
public class Recipe {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "item_a_id", nullable = false)
	private Item itemA;

	@ManyToOne(optional = false)
	@JoinColumn(name = "item_b_id", nullable = false)
	private Item itemB;

	@ManyToOne(optional = false)
	@JoinColumn(name = "result_id", nullable = false)
	private Item result;

	protected Recipe() {
	}

	public Recipe(Item itemA, Item itemB, Item result) {
		if (itemA.getId() <= itemB.getId()) {
			this.itemA = itemA;
			this.itemB = itemB;
		} else {
			this.itemA = itemB;
			this.itemB = itemA;
		}
		this.result = result;
	}

	public Long getId() {
		return id;
	}

	public Item getItemA() {
		return itemA;
	}

	public Item getItemB() {
		return itemB;
	}

	public Item getResult() {
		return result;
	}

}